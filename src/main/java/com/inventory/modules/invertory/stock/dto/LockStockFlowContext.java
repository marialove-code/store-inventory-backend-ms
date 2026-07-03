package com.inventory.modules.invertory.stock.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 锁定库存后、异步写流水所需的上下文（V3 线程池使用）。
 * <p>
 * {@link com.inventory.modules.invertory.stock.service.StockService#lockStockUpdateOnly(Long, Integer)}
 * 在同步事务内只更新 {@code lock_stock}，不写流水；本对象供异步任务调用 {@code writeFlow}。
 * </p>
 */
@Getter
@Builder
public class LockStockFlowContext {

    private final Long goodsId;
    private final String goodsName;
    private final Integer beforeLockStock;
    private final Integer changeQty;
    private final Integer afterLockStock;
}
