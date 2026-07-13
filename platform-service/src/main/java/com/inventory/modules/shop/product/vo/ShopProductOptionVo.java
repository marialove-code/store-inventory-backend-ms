package com.inventory.modules.shop.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品配件 - 开单下拉选项VO
 */
@Data
public class ShopProductOptionVo {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 默认售价
     */
    private BigDecimal salePrice;

    /**
     * 当前库存
     */
    private Integer stock;
}