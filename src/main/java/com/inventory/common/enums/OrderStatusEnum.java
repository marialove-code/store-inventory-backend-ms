package com.inventory.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "待发货"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已收货"),
    CANCELED(4, "已取消"),
    REFUNDING(5, "退款中"),  // 新增
    REFUNDED(6, "退款成功");   // 新增


    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
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