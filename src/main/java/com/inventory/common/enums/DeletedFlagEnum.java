package com.inventory.common.enums;

import lombok.Getter;

/**
 * 通用逻辑删除标记枚举。
 *
 * @author inventory
 */
@Getter
public enum DeletedFlagEnum {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    private final int code;
    private final String desc;

    DeletedFlagEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DeletedFlagEnum fromCode(Integer code) {
        if (code == null) {
            return NOT_DELETED;
        }
        for (DeletedFlagEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return NOT_DELETED;
    }
}
