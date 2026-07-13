package com.inventory.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举（从单体迁入 inventory-common，供订单服务使用）。
 * <p>
 * 状态流转概要：待支付 → 待发货 → 已发货 → 已收货；
 * 亦可进入退款中 / 退款完成 / 已取消。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "待发货"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已收货"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "退款完成"),
    CANCELED(6, "已取消");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举；未知码返回 null。
     */
    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum statusEnum : OrderStatusEnum.values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
