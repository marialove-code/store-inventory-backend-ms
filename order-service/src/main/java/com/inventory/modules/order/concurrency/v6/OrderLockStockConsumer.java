package com.inventory.modules.order.concurrency.v6;

import cn.hutool.core.bean.BeanUtil;
import com.inventory.common.exception.BusinessException;
import com.inventory.modules.order.concurrency.v4.OrderCreateConcurrencyV4SyncService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.order.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * V6 MQ 消费者：从队列取消息，异步执行「建单 + 原子锁库存」。
 * <p>
 * 【为什么消费者里用 V4 原子锁，而不是 V1 非原子？】<br>
 * V6 的教学目标是「异步解耦 / 削峰」；若消费者仍用 V1 非原子三步，
 * 多个消费者并发消费时仍可能超锁。持锁/原子更新交给 V4 链路，
 * 这样压测时正确性更稳。重复投递导致「同一请求做两次」由后续 V7 幂等解决。
 * </p>
 * <p>
 * 【最终一致】<br>
 * HTTP 已返回「已受理」之后，本方法才真正改库存；中间有短暂窗口库存未变，属预期。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLockStockConsumer {

    /**
     * 复用 V4：远程 SQL 原子 lock + 落订单（经 InventoryStockClient）。
     */
    private final OrderCreateConcurrencyV4SyncService v4SyncService;

    /**
     * 监听队列：有消息就回调本方法。
     * <p>
     * Spring 默认会：方法正常结束 → ACK（消息从队列删除）；<br>
     * 方法抛未捕获异常 → 可能重新入队（视配置而定）。<br>
     * 业务失败（库存不足）我们主动捕获并打日志，避免毒消息无限重试。
     * </p>
     *
     * @param message 生产者发出的 JSON 反序列化结果
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDER_LOCK_STOCK)
    public void onMessage(OrderLockStockMessage message) {
        // ========== 步骤 1：基本校验 ==========
        if (message == null || message.getGoodsId() == null) {
            log.warn("V6 消费者收到空消息或缺少 goodsId，直接丢弃");
            return;
        }

        log.info("V6 消费者开始处理 goodsId={} buyQty={} sentAt={}",
                message.getGoodsId(), message.getBuyQty(), message.getSentAt());

        // ========== 步骤 2：消息 → 下单 DTO ==========
        OrderInfoDTO dto = new OrderInfoDTO();
        BeanUtil.copyProperties(message, dto);

        // ========== 步骤 3：真正建单 + 原子锁库存 ==========
        try {
            String orderNo = v4SyncService.syncCreateOrderWithSqlLock(dto);
            log.info("V6 异步下单成功 orderNo={} goodsId={}", orderNo, dto.getGoodsId());
        } catch (IllegalStateException | BusinessException ex) {
            // 库存不足等业务失败：记录后 ACK，避免同一条消息反复打爆库存服务
            // （若需要「失败进死信 / 人工补偿」，留给 V7）
            log.warn("V6 异步下单业务失败 goodsId={} reason={}",
                    dto.getGoodsId(), ex.getMessage());
        } catch (Exception ex) {
            // 未知异常：打错误日志并重新抛出，让框架按策略重试（便于发现配置/网络问题）
            log.error("V6 异步下单系统异常 goodsId={}", dto.getGoodsId(), ex);
            throw ex;
        }
    }
}
