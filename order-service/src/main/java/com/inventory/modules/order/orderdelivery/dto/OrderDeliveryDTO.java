package com.inventory.modules.order.orderdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 确认发货入参 DTO。
 */
@Data
public class OrderDeliveryDTO {

    @NotBlank(message = "物流单号不能为空")
    private String logisticsNo;

    private String remark;
}
