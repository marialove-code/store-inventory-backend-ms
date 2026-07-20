package com.inventory.order.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 并发 V6 · RabbitMQ 基础设施配置。
 * <p>
 * 【读代码时先看这里】声明「队列叫什么」以及「消息怎么序列化」。
 * </p>
 * <p>
 * 教学简化：只用「默认交换机 + 队列名」直投，不引入复杂 Exchange/Binding。
 * 生产里常会加 Topic 交换机、死信队列等，留给后续加深。
 * </p>
 */
@Configuration
public class RabbitMqConfig {

    /**
     * V6 锁库存队列名。
     * <p>
     * 生产者 {@code convertAndSend(QUEUE, msg)} 与消费者 {@code @RabbitListener(queues = QUEUE)}
     * 必须用同一个名字，否则消息对不上。
     * </p>
     */
    public static final String QUEUE_ORDER_LOCK_STOCK = "order.concurrency.lock.stock";

    /**
     * 声明持久化队列（durable=true：Rabbit 重启后队列定义还在）。
     * <p>
     * Spring Boot 启动时若 Broker 上没有该队列，会自动创建。
     * </p>
     */
    @Bean
    public Queue orderLockStockQueue() {
        // 第二个参数 true = durable
        return new Queue(QUEUE_ORDER_LOCK_STOCK, true);
    }

    /**
     * 消息用 JSON 收发（OrderLockStockMessage ↔ 字节流）。
     * <p>
     * 不配的话默认是 Java 序列化，跨语言不友好，调试也不直观。
     * </p>
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
