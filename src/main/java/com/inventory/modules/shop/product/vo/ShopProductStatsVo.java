package com.inventory.modules.shop.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品配件 - 库存金额汇总
 */
@Data
public class ShopProductStatsVo {

    /**
     * 总进价（Σ 进货单价 × 当前库存）
     */
    private BigDecimal totalCostAmount;

    /**
     * 总售价（Σ 售卖单价 × 当前库存）
     */
    private BigDecimal totalSaleAmount;
}
