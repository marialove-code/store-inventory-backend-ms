package com.inventory.modules.order.concurrency.v4;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * V4：SQL 并发控制（数据库原子条件锁定库存）。
 * <p>
 * <b>实现要点</b>：
 * <ul>
 *   <li><strong>不用 JVM 锁</strong>：去掉 V2/V3 的 {@code ReentrantLock}，多线程可同时进入本方法</li>
 *   <li><strong>原子 UPDATE</strong>：{@code WHERE stock - lock_stock >= buyQty} 与加 lock_stock 在同一条 SQL</li>
 *   <li><strong>事务</strong>：原子锁库存、写流水、插入订单在同一 {@code @Transactional} 内</li>
 * </ul>
 * </p>
 * <p>
 * <b>压测目标</b>：200 并发下成功订单数 ≤ 100，{@code lockStock <= stock}，且相对 V2/V3
 * 锁排队应减轻（无 JVM 串行），RT/吞吐有望提升（以 JMeter 实测为准）。
 * </p>
 * <p>
 * <b>局限</b>：单机数据库行锁仍会有竞争；多实例时 SQL 原子条件仍有效（比 JVM 锁更适合多 Pod）。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV4 implements OrderCreateConcurrencyStrategy {

    /** V4 同步核心：SQL 原子锁库存 + 保存订单 */
    private final OrderCreateConcurrencyV4SyncService syncService;

    @Override
    public String version() {
        return ConcurrencyVersion.V4.getCode();
    }

    /**
     * V4 创建订单入口。
     * <pre>
     * 多线程可同时进入（无 ReentrantLock）：
     *   → syncService：原子 UPDATE lock_stock → 写流水 → insert order
     *   → 成功返回；库存不足 catch 后 Result.fail
     * </pre>
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        try {
            String orderNo = syncService.syncCreateOrderWithSqlLock(dto);
            return Result.success("订单创建成功，库存已锁定（V4：SQL 原子条件，订单号 " + orderNo + "）");
        } catch (IllegalStateException ex) {
            // 库存不足、商品库存不存在等业务失败
            return Result.fail(ex.getMessage());
        }
    }
}
