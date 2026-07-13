package com.inventory.modules.order.orderrefund.dto;

import lombok.Data;

/**
 * 退款审核请求 DTO。
 */
@Data
public class OrderRefundDTO {

    /** 审核备注 */
    private String remark;
}
