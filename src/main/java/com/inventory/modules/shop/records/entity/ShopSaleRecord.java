package com.inventory.modules.shop.records.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售流水记录表
 */
@Data
@TableName("shop_sale_record")
public class ShopSaleRecord {

    /**
     * 雪花主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联商品ID
     */
    private Long productId;

    /**
     * 商品名称(冗余)
     */
    private String productName;

    /**
     * 本次成交售价
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

    /**
     * 单据创建时间
     */
    private LocalDateTime createTime;
}