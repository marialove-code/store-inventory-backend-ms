package com.inventory.modules.order.concurrency.v7;

import cn.hutool.core.util.StrUtil;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.concurrency.v4.OrderCreateConcurrencyV4SyncService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * V7：幂等 +（配合补偿任务）生产级收口。
 * <p>
 * 【建议阅读顺序】
 * <ol>
 *   <li>{@link OrderIdempotentService} —— Redis 幂等占位</li>
 *   <li>本类 {@link #createOrder} —— 同步链路演示「连点只下一单」</li>
 *   <li>{@link OrderConcurrencyCompensateJob} —— 解锁失败后的定时补偿</li>
 * </ol>
 * </p>
 * <p>
 * 【为何先做同步幂等，而不是只改 V6？】<br>
 * 同步更容易压测验证：同一 {@code idempotentKey} 连打 N 次，{@code lockStock} 只应 +1。
 * V6 消费者里也会复用同一套 {@link OrderIdempotentService}（消息带幂等键时）。
 * </p>
 * <p>
 * 压测：{@code POST .../order/add?version=v7}，Body 必须带 {@code idempotentKey}。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV7 implements OrderCreateConcurrencyStrategy {

    private final OrderIdempotentService idempotentService;
    private final OrderCreateConcurrencyV4SyncService v4SyncService;

    @Override
    public String version() {
        return ConcurrencyVersion.V7.getCode();
    }

    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }
        if (dto.getBuyQty() == null || dto.getBuyQty() < 1) {
            return Result.fail("buyQty 必须大于 0");
        }
        if (StrUtil.isBlank(dto.getIdempotentKey())) {
            return Result.fail("V7 必须传 idempotentKey（幂等键，连点请保持相同）");
        }

        String key = dto.getIdempotentKey().trim();

        // ========== 1. 幂等占位 ==========
        var hit = idempotentService.tryBegin(key);
        if (hit.isPresent()) {
            var h = hit.get();
            if (h.inProgress()) {
                return Result.fail("V7：相同幂等键正在处理中，请稍后查询（key=" + key + "）");
            }
            return Result.success("V7 幂等命中，返回原订单号：" + h.orderNo());
        }

        // ========== 2. 真正建单（复用 V4 原子锁） ==========
        try {
            String orderNo = v4SyncService.syncCreateOrderWithSqlLock(dto);
            idempotentService.markDone(key, orderNo);
            return Result.success("V7 下单成功（幂等+V4），orderNo=" + orderNo);
        } catch (IllegalStateException | BusinessException ex) {
            // 库存不足等：清掉占位，允许换库存后再用同一 key 重试
            idempotentService.clear(key);
            log.warn("V7 业务失败，已清幂等 key={} reason={}", key, ex.getMessage());
            return Result.fail("V7 下单失败：" + ex.getMessage());
        } catch (Exception ex) {
            idempotentService.clear(key);
            log.error("V7 系统异常 key={}", key, ex);
            return Result.fail("V7 系统异常：" + ex.getMessage());
        }
    }
}
