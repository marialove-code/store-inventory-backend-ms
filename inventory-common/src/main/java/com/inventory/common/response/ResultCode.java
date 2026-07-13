package com.inventory.common.response;

import lombok.Getter;

/**
 * 统一业务码（微服务公共模块精简版）。
 * <p>
 * P0 仅保留成功/失败；后续从单体按需迁入更多枚举项，避免一次搬全量。
 * </p>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
