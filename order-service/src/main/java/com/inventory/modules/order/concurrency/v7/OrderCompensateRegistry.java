package com.inventory.modules.order.concurrency.v7;

import com.inventory.common.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * V7 补偿登记：本地解锁失败时，把「待解锁」写入 Redis Set，交给定时任务重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensateRegistry {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 登记一条待补偿解锁。
     *
     * @param goodsId 商品
     * @param qty     数量
     * @param orderNo 关联单号（便于日志）
     */
    public void enqueueUnlock(Long goodsId, Integer qty, String orderNo) {
        if (goodsId == null || qty == null) {
            return;
        }
        String member = goodsId + "|" + qty + "|" + (orderNo == null ? "-" : orderNo);
        stringRedisTemplate.opsForSet().add(RedisConstants.ORDER_COMPENSATE_UNLOCK_SET, member);
        log.warn("V7 登记补偿解锁 member={}", member);
    }
}
