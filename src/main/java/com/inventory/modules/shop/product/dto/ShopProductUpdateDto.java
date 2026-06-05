package com.inventory.modules.shop.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品配件 - 修改DTO（补货 + 调价）
 */
@Data
public class ShopProductUpdateDto {

    /**
     * 本次入库数量（不补货填0）
     */
    @NotNull(message = "入库数量不能为空")
    @PositiveOrZero(message = "入库数量不能小于0")
    private Integer receiptQty;

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
}