package com.inventory.modules.order.concurrency.v7;

import com.inventory.common.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * V7 幂等服务：用 Redis SET NX 保证「同一 idempotentKey 只真正下单一次」。
 * <p>
 * 【状态】
 * <ul>
 *   <li>{@code PROCESSING} —— 已有请求正在处理，后来者应提示处理中或等待</li>
 *   <li>{@code DONE:{orderNo}} —— 已成功，后来者直接返回同一订单号</li>
 * </ul>
 * </p>
 * <p>
 * 业务失败时会 {@link #clear(String)}，允许用户用同一键重试（库存不足等场景）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdempotentService {

    private static final String PROCESSING = "PROCESSING";
    private static final String DONE_PREFIX = "DONE:";

    private final StringRedisTemplate stringRedisTemplate;

    /** 幂等记录保留时长（小时），默认 24 */
    @Value("${app.concurrency.v7.idempotent-ttl-hours:24}")
    private long ttlHours;

    /**
     * 尝试占用幂等键（下单前调用）。
     * <p>
     * 返回值怎么读：
     * <ul>
     *   <li>{@code Optional.empty()} —— 抢占成功，调用方可以继续走真正的建单逻辑</li>
     *   <li>{@code Optional.of(hit)} —— 这个 key 已经有人用过：要么处理中，要么已有订单号</li>
     * </ul>
     * </p>
     *
     * @param idempotentKey 前端传来的幂等键（同一次业务应相同）
     * @return empty=可继续下单；有值=命中已有状态
     */
    public Optional<IdempotentHit> tryBegin(String idempotentKey) {
        // 1) 拼出 Redis 完整 Key，例如 inventory:idempotent:order:smoke-key-001
        String redisKey = redisKey(idempotentKey);

        // 2) SET NX + 过期时间：Key 不存在时才写入 value=PROCESSING
        //    ok=true  → 我是第一个占到的，可以去下单
        //    ok=false → Key 已存在，说明有人正在处理或已经完成过
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, PROCESSING, Duration.ofHours(ttlHours));

        // 3) 占位成功：返回 empty，告诉调用方「继续执行 V4 建单」
        if (Boolean.TRUE.equals(ok)) {
            log.info("V7 幂等占位成功 key={}", idempotentKey);
            return Optional.empty();
        }

        // 4) 占位失败：把现有 value 读出来，判断是 PROCESSING 还是 DONE:订单号
        String existing = stringRedisTemplate.opsForValue().get(redisKey);

        // 5) 极端情况：刚才判断存在，读的时候却变成 null（刚好 TTL 过期被删）
        //    再抢一次占位；抢到同样返回 empty，允许继续下单
        if (existing == null) {
            ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(redisKey, PROCESSING, Duration.ofHours(ttlHours));
            if (Boolean.TRUE.equals(ok)) {
                return Optional.empty();
            }
            // 再抢仍失败：说明一瞬间又被别人写上了，重新读 value
            existing = stringRedisTemplate.opsForValue().get(redisKey);
        }

        // 6) value 以 DONE: 开头 → 以前已经下单成功，把订单号抠出来返回
        //    调用方不应再锁库存，直接把原 orderNo 给用户
        if (existing != null && existing.startsWith(DONE_PREFIX)) {
            // "DONE:DD001".substring(5) → "DD001"
            String orderNo = existing.substring(DONE_PREFIX.length());
            log.info("V7 幂等命中已完成 key={} orderNo={}", idempotentKey, orderNo);
            return Optional.of(IdempotentHit.done(orderNo));
        }

        // 7) 剩下情况：一般是 value=PROCESSING（别人正在下单还没 markDone）
        //    返回「处理中」，调用方提示用户稍后再查，避免并发建两单
        log.info("V7 幂等命中处理中 key={}", idempotentKey);
        return Optional.of(IdempotentHit.processing());
    }

    /** 下单成功：写入 DONE:订单号 */
    public void markDone(String idempotentKey, String orderNo) {
        stringRedisTemplate.opsForValue().set(
                redisKey(idempotentKey),
                DONE_PREFIX + orderNo,
                Duration.ofHours(ttlHours));
        log.info("V7 幂等标记完成 key={} orderNo={}", idempotentKey, orderNo);
    }

    /** 业务失败：删掉占位，允许同一键重试 */
    public void clear(String idempotentKey) {
        stringRedisTemplate.delete(redisKey(idempotentKey));
        log.info("V7 幂等清除（可重试） key={}", idempotentKey);
    }

    private String redisKey(String idempotentKey) {
        return RedisConstants.ORDER_IDEMPOTENT_PREFIX + idempotentKey;
    }

    /**
     * 幂等命中结果。
     *
     * @param inProgress 是否仍在处理中（true=PROCESSING；false=已有订单号）
     * @param orderNo    已完成时的订单号
     */
    public record IdempotentHit(boolean inProgress, String orderNo) {
        static IdempotentHit processing() {
            return new IdempotentHit(true, null);
        }

        static IdempotentHit done(String orderNo) {
            return new IdempotentHit(false, orderNo);
        }
    }
}
