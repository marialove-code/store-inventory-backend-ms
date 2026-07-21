package com.inventory.modules.order.concurrency.v6;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.inventory.common.exception.BusinessException;
import com.inventory.modules.order.concurrency.v4.OrderCreateConcurrencyV4SyncService;
import com.inventory.modules.order.concurrency.v7.OrderIdempotentService;
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
 * 多个消费者并发消费时仍可能超锁。持锁/原子更新交给 V4 链路。
 * </p>
 * <p>
 * 【V7 叠加】消息带 {@code idempotentKey} 时，先走 {@link OrderIdempotentService}，
 * 避免 Broker 重复投递导致锁两次。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLockStockConsumer {

    private final OrderCreateConcurrencyV4SyncService v4SyncService;
    private final OrderIdempotentService idempotentService;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDER_LOCK_STOCK)
    public void onMessage(OrderLockStockMessage message) {
        if (message == null || message.getGoodsId() == null) {
            log.warn("V6 消费者收到空消息或缺少 goodsId，直接丢弃");
            return;
        }

        log.info("V6 消费者开始处理 goodsId={} buyQty={} sentAt={} idempotentKey={}",
                message.getGoodsId(), message.getBuyQty(), message.getSentAt(),
                message.getIdempotentKey());

        OrderInfoDTO dto = new OrderInfoDTO();
        BeanUtil.copyProperties(message, dto);

        String idempotentKey = StrUtil.trim(message.getIdempotentKey());
        boolean useIdempotent = StrUtil.isNotBlank(idempotentKey);

        if (useIdempotent) {
            var hit = idempotentService.tryBegin(idempotentKey);
            if (hit.isPresent()) {
                if (hit.get().inProgress()) {
                    log.info("V6 幂等：处理中，跳过重复消息 key={}", idempotentKey);
                } else {
                    log.info("V6 幂等：已完成 orderNo={} key={}", hit.get().orderNo(), idempotentKey);
                }
                return;
            }
        }

        try {
            String orderNo = v4SyncService.syncCreateOrderWithSqlLock(dto);
            if (useIdempotent) {
                idempotentService.markDone(idempotentKey, orderNo);
            }
            log.info("V6 异步下单成功 orderNo={} goodsId={}", orderNo, dto.getGoodsId());
        } catch (IllegalStateException | BusinessException ex) {
            if (useIdempotent) {
                idempotentService.clear(idempotentKey);
            }
            log.warn("V6 异步下单业务失败 goodsId={} reason={}",
                    dto.getGoodsId(), ex.getMessage());
        } catch (Exception ex) {
            if (useIdempotent) {
                idempotentService.clear(idempotentKey);
            }
            log.error("V6 异步下单系统异常 goodsId={}", dto.getGoodsId(), ex);
            throw ex;
        }
    }
}
