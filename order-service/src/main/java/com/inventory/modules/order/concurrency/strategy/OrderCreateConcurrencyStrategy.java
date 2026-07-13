package com.inventory.modules.order.concurrency.strategy;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;

/**
 * 订单创建并发策略接口。
 * <p>
 * V1～V7 各版本独立实现本接口，由 {@link com.inventory.modules.order.concurrency.facade.OrderConcurrencyFacade}
 * 按 {@code version} 参数路由到对应实现，便于压测对比与面试讲解演进过程。
 * </p>
 *
 * @author 95349
 */
public interface OrderCreateConcurrencyStrategy {

    /**
     * 版本标识，与 URL 参数 {@code version} 一致，如 {@code v1}、{@code v5}。
     *
     * @return 小写版本号
     */
    String version();

    /**
     * 按当前版本策略创建订单（含锁库存逻辑）。
     *
     * @param dto 与正式接口 {@code POST /order/info/add} 相同的入参
     * @return 统一返回体；成功 code=0，库存不足等业务失败返回 fail
     */
    Result<?> createOrder(OrderInfoDTO dto);
}
