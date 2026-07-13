package com.inventory.modules.order.orderrefund.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单实体（对应 order_refund）。
 */
@TableName(value = "order_refund")
@Data
public class OrderRefund {

    @TableId
    private Long id;

    /** 关联订单 ID */
    private Long orderId;

    private String orderNo;
    private String userName;
    private String goodsName;
    private BigDecimal refundAmount;

    /** 退款状态：0-待审核 1-通过 2-拒绝 */
    private Integer refundStatus;

    private LocalDateTime applyTime;
    private String auditRemark;
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 退款前订单原始状态（拒绝时还原） */
    private Integer originalOrderStatus;

    /** 退款前发货单原始状态（拒绝时还原） */
    private Integer originalDeliveryStatus;
}
