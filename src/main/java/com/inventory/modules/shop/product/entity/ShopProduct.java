package com.inventory.modules.shop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品配件信息表
 */
@Data
@TableName("shop_product")
public class ShopProduct {

    /**
     * 雪花主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 商品配件名称
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
     * 商品来源
     */
    private String source;

    /**
     * 库存预警值
     */
    private Integer stockWarn;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}