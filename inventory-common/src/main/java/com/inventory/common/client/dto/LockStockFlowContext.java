package com.inventory.common.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 锁定库存后、异步写流水所需的上下文（跨服务 DTO）。
 * <p>
 * 由 inventory-service {@code lockStockUpdateOnly} 返回；
 * order-service V3 异步任务据此调用 {@code writeFlow}。
 * 放在 inventory-common，避免 order-service 依赖 inventory-service 模块类。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockStockFlowContext {

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称（写流水冗余） */
    private String goodsName;

    /** 锁定前的 lock_stock */
    private Integer beforeLockStock;

    /** 本次锁定数量 */
    private Integer changeQty;

    /** 锁定后的 lock_stock */
    private Integer afterLockStock;
}
