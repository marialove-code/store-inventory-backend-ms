package com.inventory.modules.order.orderdelivery.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName order_delivery
 */
@TableName(value ="order_delivery")
@Data
public class OrderDelivery {
    /**
     * 订单主键
     */
    @TableId
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 购买数量
     */
    private Integer buyQty;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 订单状态：1-已支付 2-已发货
     */
    private Integer orderStatus;

    /**
     * 物流单号
     */
    private String logisticsNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序号，数字越小越靠前
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}