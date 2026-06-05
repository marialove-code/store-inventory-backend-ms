package com.inventory.modules.shop.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;


import java.math.BigDecimal;

/**
 * 商品配件 - 新增DTO
 */
@Data
public class ShopProductCreateDto {

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    /**
     * 进货单价
     */
    @NotNull(message = "进货单价不能为空")
    @PositiveOrZero(message = "进货单价不能小于0")
    private BigDecimal costPrice;

    /**
     * 售卖单价
     */
    @NotNull(message = "售卖单价不能为空")
    @PositiveOrZero(message = "售卖单价不能小于0")
    private BigDecimal salePrice;

    /**
     * 初始库存
     */
    @NotNull(message = "初始库存不能为空")
    @PositiveOrZero(message = "库存不能小于0")
    private Integer stock;

    /**
     * 商品来源
     */
    private String source;
}