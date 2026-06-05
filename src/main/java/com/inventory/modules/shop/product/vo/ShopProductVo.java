package com.inventory.modules.shop.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品配件 - 列表VO
 */
@Data
public class ShopProductVo {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 进货单价
     */
    private BigDecimal costPrice;

    /**
     * 售卖单价
     */
    private BigDecimal salePrice;

    /**
     * 当前库存
     */
    private Integer stock;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}