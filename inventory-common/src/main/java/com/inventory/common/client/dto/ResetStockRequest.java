package com.inventory.common.client.dto;

import lombok.Data;

/**
 * 压测辅助：重置商品库存请求（跨服务 DTO）。
 * <p>
 * 对应 inventory-service {@code POST /inventory/internal/dev/reset-stock}。
 * <b>仅压测辅助，后续可加 Profile 限制。</b>
 * </p>
 */
@Data
public class ResetStockRequest {

    /** 商品 ID */
    private Long goodsId;

    /** 重置后的账面库存 */
    private Integer stock;

    /** 重置后的锁定库存 */
    private Integer lockStock;
}
