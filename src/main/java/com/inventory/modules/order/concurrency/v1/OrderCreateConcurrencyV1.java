package com.inventory.modules.order.concurrency.v1;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * V1：单线程基础版（无额外并发控制）。
 * <p>
 * <b>实现说明</b>：直接委托现有 {@link OrderInfoService#createOrder}，
 * 即当前单体项目中「读可用库存 → 判断 → 写订单 → lockStock」的默认逻辑，
 * 未加 synchronized、乐观锁、Redis 锁等任何并发手段。
 * </p>
 * <p>
 * <b>压测预期</b>：高并发下可能出现「锁定库存 &gt; 实际可售数量」的超锁现象，
 * 用于与 V2～V7 对比。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV1 implements OrderCreateConcurrencyStrategy {

    private final OrderInfoService orderInfoService;

    @Override
    public String version() {
        return ConcurrencyVersion.V1.getCode();
    }

    /**
     * V1 创建订单：复用正式业务 Service，不做任何并发改造。
     *
     * @param dto 下单入参
     * @return 与 {@code POST /order/info/add} 一致
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        // V1 基线：完全沿用现有实现
        return orderInfoService.createOrder(dto);
    }
}
