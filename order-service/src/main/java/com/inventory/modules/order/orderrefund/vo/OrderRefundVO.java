package com.inventory.modules.order.orderrefund.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单列表/详情 VO。
 */
@Data
public class OrderRefundVO {

    private Long id;
    private Long orderId;
    private String orderNo;
    private String userName;
    private String goodsName;
    private BigDecimal refundAmount;
    private Integer refundStatus;
    private String statusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    private String auditRemark;
}
