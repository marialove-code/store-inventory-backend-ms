package com.inventory.modules.order.concurrency.v6;

import org.springframework.stereotype.Component;

/**
 * V6 MQ 消费者：异步执行锁库存。
 * <p>
 * <b>计划实现</b>（待开发）：
 * <ul>
 *   <li>监听队列，如 {@code order.lock.stock}</li>
 *   <li>消费消息：创建订单记录 + 调用 {@code StockService#lockStock}</li>
 *   <li>失败进入死信或重试队列，由 V7 补偿任务兜底</li>
 * </ul>
 * </p>
 */
@Component
public class OrderLockStockConsumer {

    /**
     * TODO: 使用 @RabbitListener 或 @RocketMQMessageListener 接收消息并锁库存。
     *
     * @param messagePayload 消息体（订单 DTO 或 orderId + goodsId + buyQty）
     */
    public void onMessage(String messagePayload) {
        // 占位：V6 开发时实现
        throw new UnsupportedOperationException("V6 消费者待实现");
    }
}
