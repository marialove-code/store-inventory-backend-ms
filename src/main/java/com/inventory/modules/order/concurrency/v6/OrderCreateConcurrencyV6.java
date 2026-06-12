package com.inventory.modules.order.concurrency.v6;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import org.springframework.stereotype.Component;

/**
 * V6：MQ 最终一致性（异步解耦）。
 * <p>
 * <b>计划实现</b>（待开发）：
 * <ul>
 *   <li>接口层：校验参数 → 发送「创建订单/锁库存」消息 → 快速返回（或返回受理中）</li>
 *   <li>消费者：{@link OrderLockStockConsumer} 异步执行 lockStock + 落库</li>
 *   <li>配合 V7 处理重复消费、失败重试</li>
 * </ul>
 * </p>
 * <p>
 * 消费者类见同包 {@link OrderLockStockConsumer}。
 * </p>
 */
@Component
public class OrderCreateConcurrencyV6 implements OrderCreateConcurrencyStrategy {

    @Override
    public String version() {
        return ConcurrencyVersion.V6.getCode();
    }

    /**
     * TODO: 发送 MQ 消息，由消费者完成锁库存；接口 RT 应明显低于同步链路。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        return Result.fail("V6 待实现：MQ 异步锁库存");
    }
}
