package com.inventory.modules.order.concurrency.v7;

import com.inventory.common.constants.RedisConstants;
import com.inventory.order.client.InventoryStockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * V7 补偿定时任务：重试「锁库存成功但本地落单失败后、远程解锁又失败」的残留。
 * <p>
 * 【典型不一致】库存已 lock，订单没写入，且当时 unlock HTTP 也失败 → 库存被多锁一截。<br>
 * 登记见 {@link OrderCompensateRegistry}；本任务周期性取出 Redis Set 再调 unlock。
 * </p>
 * <p>
 * 仅 {@code dev} 打开，避免学习环境误伤生产。
 * </p>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class OrderConcurrencyCompensateJob {

    private final StringRedisTemplate stringRedisTemplate;
    private final InventoryStockClient inventoryStockClient;

    /**
     * 默认每 30 秒扫一批（可用配置覆盖）。
     */
    @Scheduled(fixedDelayString = "${app.concurrency.v7.compensate-delay-ms:30000}")
    public void runCompensate() {
        Set<String> members = stringRedisTemplate.opsForSet()
                .members(RedisConstants.ORDER_COMPENSATE_UNLOCK_SET);
        if (members == null || members.isEmpty()) {
            return;
        }

        log.info("V7 补偿任务开始，待处理 {} 条", members.size());
        int ok = 0;
        int fail = 0;
        for (String member : members) {
            if (tryUnlockOne(member)) {
                stringRedisTemplate.opsForSet()
                        .remove(RedisConstants.ORDER_COMPENSATE_UNLOCK_SET, member);
                ok++;
            } else {
                fail++;
            }
        }
        log.info("V7 补偿任务结束 success={} fail={}", ok, fail);
    }

    /**
     * member 格式：{@code goodsId|qty|orderNo}
     */
    private boolean tryUnlockOne(String member) {
        try {
            String[] parts = member.split("\\|");
            if (parts.length < 2) {
                log.warn("V7 补偿 member 格式非法，丢弃：{}", member);
                return true;
            }
            Long goodsId = Long.valueOf(parts[0]);
            Integer qty = Integer.valueOf(parts[1]);
            String orderNo = parts.length >= 3 ? parts[2] : "-";
            inventoryStockClient.unlock(goodsId, qty);
            log.info("V7 补偿解锁成功 orderNo={} goodsId={} qty={}", orderNo, goodsId, qty);
            return true;
        } catch (Exception ex) {
            log.warn("V7 补偿解锁失败 member={} reason={}", member, ex.getMessage());
            return false;
        }
    }
}
