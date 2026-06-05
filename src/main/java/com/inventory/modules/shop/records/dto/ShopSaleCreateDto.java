package com.inventory.modules.shop.records.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售开单 - DTO
 */
@Data
public class ShopSaleCreateDto {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量 ≥1
     */
    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 实际售价（可改价，不传则使用商品默认售价）
     */
    @PositiveOrZero(message = "售价不能小于0")
    private BigDecimal salePrice;
}