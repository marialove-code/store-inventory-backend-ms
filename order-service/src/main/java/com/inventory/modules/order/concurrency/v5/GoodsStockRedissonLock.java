package com.inventory.modules.order.concurrency.v5;

import com.inventory.common.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 商品维度 Redisson 分布式锁（V5r）—— 对照 {@link GoodsStockRedisLock} 读。
 * <p>
 * 【和手写 V5 比，你要抓住的两点】
 * <ol>
 *   <li>仍然是「Redis 上的分布式锁」，多实例有效</li>
 *   <li>leaseTime=-1 时启用<strong>看门狗</strong>：持锁期间自动续期，
 *       业务偶尔超过 30s 也不容易因为固定 EX 到期而提前丢锁</li>
 * </ol>
 * 手写版要自己管 token + Lua；Redisson 把可重入、续期、解锁安全封装进 {@link RLock}。
 * </p>
 * <p>
 * Key 带 {@code redisson:} 后缀，避免和手写版 Key 混在同一次压测里互相抢。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoodsStockRedissonLock {

    /** Redisson 客户端（Spring Boot starter 自动配置） */
    private final RedissonClient redissonClient;

    /**
     * 尝试加锁。
     * <p>
     * 【成功】返回 {@link RLock} 对象（解锁时要用同一引用）<br>
     * 【失败】返回 null（waitMs 内没抢到）
     * </p>
     *
     * @param waitMs       最多等待多久（毫秒）
     * @param leaseSeconds &lt;=0：看门狗自动续期；&gt;0：固定租约秒数（更像手写 V5）
     */
    public RLock tryLock(Long goodsId, long waitMs, long leaseSeconds) {
        // 步骤 1：按商品拿到一把逻辑锁对象（尚未真正加锁）
        RLock lock = redissonClient.getLock(RedisConstants.STOCK_LOCK_PREFIX + "redisson:" + goodsId);
        try {
            boolean ok;
            if (leaseSeconds <= 0) {
                // 步骤 2a：看门狗模式
                // tryLock(waitTime, leaseTime=-1, unit)
                // leaseTime=-1 → Redisson 默认约 30s TTL，并在持锁期间定时续期（约每 10s）
                ok = lock.tryLock(waitMs, -1, TimeUnit.MILLISECONDS);
            } else {
                // 步骤 2b：固定租约（对照手写 EX），到期不续期
                ok = lock.tryLock(waitMs, leaseSeconds, TimeUnit.SECONDS);
            }
            // 步骤 3：成功返回 lock，失败返回 null 给上层变「繁忙」
            return ok ? lock : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 解锁：仅当前线程仍持有时才 unlock，避免乱解锁抛异常。
     */
    public void unlock(RLock lock) {
        if (lock == null) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception ex) {
            log.warn("V5r Redisson 解锁异常：{}", ex.getMessage());
        }
    }
}
