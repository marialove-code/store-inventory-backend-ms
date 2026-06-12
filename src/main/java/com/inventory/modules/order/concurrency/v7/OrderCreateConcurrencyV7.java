package com.inventory.modules.order.concurrency.v7;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import org.springframework.stereotype.Component;

/**
 * V7：幂等 + 补偿机制（生产级完善）。
 * <p>
 * <b>计划实现</b>（待开发）：
 * <ul>
 *   <li><b>幂等</b>：客户端传 {@code idempotentKey} 或订单号唯一约束，防止重复提交重复锁库存</li>
 *   <li><b>重试</b>：MQ 消费失败指数退避重试</li>
 *   <li><b>补偿</b>：{@link OrderConcurrencyCompensateJob} 扫描「订单已建但 lock 失败」等不一致状态并修复</li>
 * </ul>
 * </p>
 * <p>
 * 可在 V6 基础上叠加，也可在 V5 同步链路上先只做幂等。
 * </p>
 */
@Component
public class OrderCreateConcurrencyV7 implements OrderCreateConcurrencyStrategy {

    @Override
    public String version() {
        return ConcurrencyVersion.V7.getCode();
    }

    /**
     * TODO: 在 V5/V6 方案上增加幂等校验与异常补偿入口。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        return Result.fail("V7 待实现：幂等 + 补偿");
    }
}
