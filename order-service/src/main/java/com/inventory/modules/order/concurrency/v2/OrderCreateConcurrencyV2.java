package com.inventory.modules.order.concurrency.v2;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.concurrency.v1.OrderCreateConcurrencyV1SyncService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * V2：用 JVM 内 {@link ReentrantLock} 按商品互斥（读代码时请和 V5 对照）。
 * <p>
 * 【和 V5 长得几乎一样】都是：加锁 → {@code syncCreateOrderNonAtomic} → finally 解锁。<br>
 * 【唯一关键差别】锁对象在不在本机内存：
 * <ul>
 *   <li>V2：{@code ConcurrentHashMap} 里的 {@code ReentrantLock} —— 只对本进程有效</li>
 *   <li>V5：Redis Key —— 所有 order 实例共享</li>
 * </ul>
 * </p>
 * <p>
 * 【局限】部署 2 个 order-service 时，各 JVM 各有一把锁，互不感知 → 仍可能超锁。
 * 这正是要做 V5 的原因。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV2 implements OrderCreateConcurrencyStrategy {

    /** 与 V5 相同：临界区复用 V1 非原子建单 */
    private final OrderCreateConcurrencyV1SyncService v1SyncService;

    /**
     * goodsId → 该商品专属的 JVM 锁。
     * 静态 Map：整个进程共享；换一个进程就另有一份 Map（多实例失效的根源）。
     */
    private static final ConcurrentHashMap<Long, ReentrantLock> GOODS_LOCKS = new ConcurrentHashMap<>();

    /** 没有则创建一把新的 ReentrantLock，保证「同一商品一把锁」 */
    private static ReentrantLock lockForGoods(Long goodsId) {
        return GOODS_LOCKS.computeIfAbsent(goodsId, id -> new ReentrantLock());
    }

    @Override
    public String version() {
        return ConcurrencyVersion.V2.getCode();
    }

    /**
     * V2 主流程：逐步对照 V5 的 createOrder 看。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        // 步骤 1：校验
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        // 步骤 2：取出本 JVM 内该商品的锁（不是 Redis）
        ReentrantLock lock = lockForGoods(dto.getGoodsId());

        // 步骤 3：加锁（抢不到会阻塞等待，不像 V5 有 waitMs 超时返回）
        lock.lock();
        try {
            // 步骤 4：临界区 —— 与 V5 完全相同的业务方法
            String orderNo = v1SyncService.syncCreateOrderNonAtomic(dto);
            return Result.success("订单创建成功，库存已锁定（V2，订单号 " + orderNo + "）");
        } catch (IllegalStateException | BusinessException ex) {
            return Result.fail(ex.getMessage());
        } finally {
            // 步骤 5：释放 JVM 锁
            lock.unlock();
        }
    }
}
