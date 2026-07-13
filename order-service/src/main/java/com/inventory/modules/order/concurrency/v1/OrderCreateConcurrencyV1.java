package com.inventory.modules.order.concurrency.v1;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * V1：单线程基础版（无额外并发控制）——复现单体超锁基线。
 * <p>
 * <b>实现说明</b>：委托 {@link OrderCreateConcurrencyV1SyncService}，
 * 流程为「读可用库存 → 写订单 → 非原子 lockStock」，
 * <b>不</b>调用微服务正式 {@code createOrder}（正式入口已是先原子锁）。
 * </p>
 * <p>
 * <b>压测预期</b>：高并发下可能出现「锁定库存 &gt; 实际可售数量」的超锁现象，
 * 用于与 V2～V7 对比。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV1 implements OrderCreateConcurrencyStrategy {

    private final OrderCreateConcurrencyV1SyncService v1SyncService;

    @Override
    public String version() {
        return ConcurrencyVersion.V1.getCode();
    }

    /**
     * V1 创建订单：无 JVM 锁、无 SQL 原子条件，复现超锁基线。
     *
     * @param dto 下单入参
     * @return 成功或库存不足等业务失败
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }
        try {
            String orderNo = v1SyncService.syncCreateOrderNonAtomic(dto);
            return Result.success("订单创建成功，库存已锁定（V1 基线，订单号 " + orderNo + "）");
        } catch (IllegalStateException | BusinessException ex) {
            return Result.fail(ex.getMessage());
        }
    }
}
