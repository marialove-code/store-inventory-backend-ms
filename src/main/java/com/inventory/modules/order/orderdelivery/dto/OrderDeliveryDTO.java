package com.inventory.modules.order.orderdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * 订单发货DTO
 */
@Data
public class OrderDeliveryDTO {

    /**
     * 物流单号
     */
    @NotBlank(message = "物流单号不能为空")
    private String logisticsNo;

    /**
     * 备注
     */
    private String remark;
}