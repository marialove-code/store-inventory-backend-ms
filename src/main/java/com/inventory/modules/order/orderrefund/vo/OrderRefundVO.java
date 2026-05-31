package com.inventory.modules.order.orderrefund.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款订单VO
 * 用于列表和详情页数据展示
 */
@Data
public class OrderRefundVO {

    /**
     * 退款单主键ID
     */
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
     * 退款状态（0-待审核 1-已通过 2-已拒绝）
     */
    private Integer refundStatus;

    /**
     * 退款状态文案（前端展示）
     */
    private String statusName;

    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    /**
     * 审核备注
     */
    private String auditRemark;
}