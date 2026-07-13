package com.inventory.modules.order.orderrefund.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起退款申请 DTO。
 */
@Data
public class OrderRefundApplyDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 退款金额，默认订单金额 */
    private BigDecimal refundAmount;

    /** 申请原因 */
    private String remark;
}
