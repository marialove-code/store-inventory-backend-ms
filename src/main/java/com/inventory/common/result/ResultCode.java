package com.inventory.common.result;

import lombok.Getter;

/**
 * 全局统一返回码枚举（与具体业务无关）。
 *
 * @author inventory
 */
@Getter
public enum ResultCode {

    // ==================== 基础 ====================
    SUCCESS(0, "成功"),
    FAIL(1, "操作失败"),

    // ==================== 前端常用业务错误 ====================
    USER_EXIST(1001, "用户名已存在"),
    USER_NOT_EXIST(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_LOCKED(1004, "账号已被锁定"),

    // ==================== 自定义 HTTP 错误 ====================
    PARAM_ERROR(10400, "参数错误"),
    UNAUTHORIZED(10401, "未登录或token已过期"),
    FORBIDDEN(10403, "无权限访问"),
    NOT_FOUND(10404, "资源不存在"),
    SYSTEM_ERROR(10500, "系统繁忙，请稍后再试");
    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
