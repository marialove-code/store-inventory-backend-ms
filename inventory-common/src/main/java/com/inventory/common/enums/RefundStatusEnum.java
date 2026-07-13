package com.inventory.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退款状态枚举
 */
@Getter
@AllArgsConstructor
public enum RefundStatusEnum {

    /**
     * 待审核
     */
    PENDING(0, "待审核"),

    /**
     * 审核通过
     */
    APPROVED(1, "已通过"),

    /**
     * 已拒绝
     */
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return 枚举对象
     */
    public static RefundStatusEnum getByCode(Integer code) {
        for (RefundStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}