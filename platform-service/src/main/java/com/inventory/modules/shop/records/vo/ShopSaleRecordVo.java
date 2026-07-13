package com.inventory.modules.shop.records.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售流水 - 列表VO
 */
@Data
public class ShopSaleRecordVo {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 售卖单价
     */
    private BigDecimal salePrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 实收总金额
     */
    private BigDecimal totalAmount;

    /**
     * 单笔利润
     */
    private BigDecimal profit;

    /**
     * 售卖时间
     */
    private LocalDateTime saleTime;
}