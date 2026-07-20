package com.inventory.modules.order.concurrency.v6;

import cn.hutool.core.bean.BeanUtil;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.order.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * V6 策略入口（生产者）：校验 → 发 MQ → 快速返回「已受理」。
 * <p>
 * 【建议阅读顺序】
 * <ol>
 *   <li>{@link RabbitMqConfig} —— 队列名、JSON 转换</li>
 *   <li>本类 {@link #createOrder} —— 接口侧只负责投递</li>
 *   <li>{@link OrderLockStockConsumer} —— 消费者真正建单+锁库存</li>
 * </ol>
 * </p>
 * <p>
 * 【和 V5 的差别】
 * <pre>
 * V5：请求线程自己抢 Redis 锁 → 同步建单锁库存 → 再返回（RT 可能很长）
 * V6：请求线程只发消息 → 立刻返回；消费者异步完成业务（最终一致）
 * </pre>
 * </p>
 * <p>
 * 压测：{@code POST /api/order/concurrency/order/add?version=v6}
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV6 implements OrderCreateConcurrencyStrategy {

    /**
     * Spring AMQP 提供的发消息模板（类似 Redis 的 StringRedisTemplate）。
     */
    private final RabbitTemplate rabbitTemplate;

    @Override
    public String version() {
        return ConcurrencyVersion.V6.getCode();
    }

    /**
     * V6 创建订单（异步受理）。
     * <p>
     * 注意：这里<strong>不</strong>写订单、不锁库存；那些在消费者里做。
     * 因此返回成功只表示「消息已发出」，不保证库存此时已经扣上（最终一致）。
     * </p>
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        // ========== 步骤 1：参数校验 ==========
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }
        if (dto.getBuyQty() == null || dto.getBuyQty() < 1) {
            return Result.fail("buyQty 必须大于 0");
        }

        // ========== 步骤 2：组装消息体 ==========
        OrderLockStockMessage message = new OrderLockStockMessage();
        BeanUtil.copyProperties(dto, message);
        message.setSentAt(System.currentTimeMillis());

        // ========== 步骤 3：发到 Rabbit 队列 ==========
        // 使用默认交换机：routingKey = 队列名即可投递到该队列
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.QUEUE_ORDER_LOCK_STOCK, message);
        } catch (Exception ex) {
            // Broker 没起、网络不通时会进这里
            log.error("V6 发送 MQ 失败 goodsId={}", dto.getGoodsId(), ex);
            return Result.fail("V6：发送消息失败，请确认 RabbitMQ 已启动：" + ex.getMessage());
        }

        log.info("V6 消息已投递 queue={} goodsId={} buyQty={}",
                RabbitMqConfig.QUEUE_ORDER_LOCK_STOCK, dto.getGoodsId(), dto.getBuyQty());

        // ========== 步骤 4：立即返回（不等消费者做完） ==========
        return Result.success("订单已受理（V6：RabbitMQ 异步锁库存，goodsId="
                + dto.getGoodsId() + "，请稍后查库存/订单）");
    }
}
