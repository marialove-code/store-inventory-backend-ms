package com.inventory.modules.order.concurrency.v5;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.concurrency.v1.OrderCreateConcurrencyV1SyncService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * V5 策略入口：先抢 Redis 分布式锁，再执行「V1 非原子建单」临界区。
 * <p>
 * 【建议阅读顺序】
 * <ol>
 *   <li>{@link GoodsStockRedisLock} —— 锁怎么加、怎么解</li>
 *   <li>本类 {@link #createOrder} —— 业务怎么套在锁外面</li>
 *   <li>{@link OrderCreateConcurrencyV1SyncService#syncCreateOrderNonAtomic} —— 临界区里三步干啥</li>
 *   <li>{@link com.inventory.modules.order.concurrency.v2.OrderCreateConcurrencyV2} —— 对照：V2 只是把 Redis 换成 JVM 锁</li>
 *   <li>{@link OrderCreateConcurrencyV5Redisson} —— 对照：同样流程，锁换成 Redisson</li>
 * </ol>
 * </p>
 * <p>
 * 【整条请求链路（压测）】
 * <pre>
 * POST /api/order/concurrency/order/add?version=v5
 *   → DevController
 *   → OrderConcurrencyFacade（按 version 找策略）
 *   → 本类 createOrder
 *        ① tryLock(goodsId)     ← Redis
 *        ② syncCreateOrderNonAtomic  ← 读库存/建单/非原子锁库存
 *        ③ finally unlock       ← Redis
 * </pre>
 * </p>
 * <p>
 * 【和 V2】外壳都是「加锁 → 同一套 V1 业务 → 解锁」；差别只在锁实现（JVM vs Redis）。
 * 【和 V4】V4 不用这把应用锁，靠 SQL 条件 UPDATE；V5 演示的是应用层分布式锁。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV5 implements OrderCreateConcurrencyStrategy {

    /** 临界区业务：与 V1/V2 相同的非原子链路（刻意如此，才能对比「只换锁」的效果） */
    private final OrderCreateConcurrencyV1SyncService v1SyncService;

    /** 手写 Redis 锁工具 */
    private final GoodsStockRedisLock goodsStockRedisLock;

    /** 抢锁最长等待（毫秒），对应配置 app.concurrency.v5.lock-wait-ms */
    @Value("${app.concurrency.v5.lock-wait-ms:30000}")
    private long lockWaitMs;

    /** 锁租约（秒），对应 app.concurrency.v5.lock-lease-seconds；到期自动释放防死锁 */
    @Value("${app.concurrency.v5.lock-lease-seconds:30}")
    private long lockLeaseSeconds;

    @Override
    public String version() {
        // Facade 用这个字符串注册策略；URL 上 ?version=v5 会路由到本类
        return ConcurrencyVersion.V5.getCode();
    }

    /**
     * V5 创建订单主流程（逐步看注释）。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        // ========== 步骤 1：参数校验 ==========
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        // ========== 步骤 2：抢 Redis 分布式锁（按商品） ==========
        // 同一 goodsId：同一时刻只有一个请求能拿到 token
        // 拿不到（等到 waitMs 仍失败）→ token == null → 返回繁忙（压测里常见）
        String token = goodsStockRedisLock.tryLock(dto.getGoodsId(), lockWaitMs, lockLeaseSeconds);
        if (token == null) {
            return Result.fail("系统繁忙，获取商品库存锁超时（V5 Redis），请稍后重试");
        }

        try {
            // ========== 步骤 3：进入临界区（持锁期间） ==========
            // 这里面仍然是「读可用 → 建单 → 非原子 lockStock」
            // 因为外层已经互斥，多线程不会再像 V1 那样同时读到同一可用库存
            String orderNo = v1SyncService.syncCreateOrderNonAtomic(dto);
            return Result.success("订单创建成功，库存已锁定（V5：Redis 分布式锁，订单号 " + orderNo + "）");
        } catch (IllegalStateException | BusinessException ex) {
            // 库存不足等业务失败：锁仍会在 finally 释放，让别的请求有机会进来
            return Result.fail(ex.getMessage());
        } finally {
            // ========== 步骤 4：无论成功失败，必须释放锁 ==========
            // 用加锁时拿到的 token，避免误删别人的锁
            goodsStockRedisLock.unlock(dto.getGoodsId(), token);
        }
    }
}
