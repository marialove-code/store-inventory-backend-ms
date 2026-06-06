package com.inventory.modules.order.orderrefund.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 
 * @TableName order_refund
 */
@TableName(value ="order_refund")
@Data
public class OrderRefund {
    /**
     * 退款单主键
     */
    @TableId
    private Long id;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款状态：0-待审核 1-审核通过 2-已拒绝
     */
    private Integer refundStatus;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 排序号，数字越小越靠前
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 退款前订单原始状态 1-待发货 2-已发货 3-已收货
     */
    private Integer originalOrderStatus;

    /**
     * 退款前发货单原始状态 1-待发货 2-已发货 3-已收货
     */
    private Integer originalDeliveryStatus;
}