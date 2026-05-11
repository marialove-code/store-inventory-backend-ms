package com.inventory.common.result;

import lombok.Getter;

/**
 * 全局统一返回码枚举
 *
 * 当前适用：
 * - 用户管理
 * - RBAC权限
 * - Spring Security
 * - JWT认证
 * - Redis登录态
 *
 * @author inventory
 */
@Getter
public enum ResultCode {

    // =========================================================
    // 基础状态
    // =========================================================

    SUCCESS(200, "操作成功"),

    FAIL(500, "操作失败"),

    // =========================================================
    // 用户模块（1000~1099）
    // =========================================================

    USER_EXIST(1001, "用户名已存在"),

    USER_NOT_EXIST(1002, "用户不存在"),

    PASSWORD_ERROR(1003, "密码错误"),

    ACCOUNT_LOCKED(1004, "账号已被锁定"),

    LOGIN_ERROR(1005, "用户名或密码错误"),

    ACCOUNT_DISABLED(1006, "账号已被禁用"),

    OLD_PASSWORD_ERROR(1007, "原密码错误"),

    PASSWORD_NOT_MATCH(1008, "两次密码输入不一致"),

    // ====================== 【已补全】 ======================
    USERNAME_EXIST(1009, "用户名已存在"),


    // =========================================================
    // Token / 认证模块（1100~1199）
    // =========================================================

    TOKEN_INVALID(1101, "Token无效"),

    TOKEN_EXPIRED(1102, "Token已过期"),

    TOKEN_EMPTY(1103, "Token不能为空"),

    TOKEN_PARSE_ERROR(1104, "Token解析失败"),

    REFRESH_TOKEN_INVALID(1105, "RefreshToken无效"),

    REFRESH_TOKEN_EXPIRED(1106, "RefreshToken已过期"),

    LOGIN_STATUS_INVALID(1107, "登录状态不存在"),

    ACCOUNT_OFFLINE(1108, "账号已下线"),

    // ====================== 【已补全】 ======================
    NOT_LOGIN(1109, "未登录或登录已过期"),


    // =========================================================
    // 权限模块（1200~1299）
    // =========================================================

    NO_PERMISSION(1201, "无权限访问"),

    ROLE_NOT_EXIST(1202, "角色不存在"),

    PERMISSION_NOT_EXIST(1203, "权限不存在"),

    SUPER_ADMIN_NOT_ALLOW_DELETE(1204, "超级管理员不允许删除"),

    // =========================================================
    // 参数模块（1300~1399）
    // =========================================================

    PARAM_ERROR(1301, "参数错误"),

    PARAM_MISSING(1302, "缺少必要参数"),

    PARAM_TYPE_ERROR(1303, "参数类型错误"),

    REQUEST_REPEAT(1304, "请求过于频繁"),

    // =========================================================
    // HTTP状态错误（10400+）
    // =========================================================

    BAD_REQUEST(10400, "请求参数错误"),

    UNAUTHORIZED(10401, "未登录或Token已失效"),

    FORBIDDEN(10403, "无权限访问"),

    NOT_FOUND(10404, "请求资源不存在"),

    METHOD_NOT_ALLOWED(10405, "请求方式不允许"),

    // =========================================================
    // 系统错误（10500+）
    // =========================================================

    SYSTEM_ERROR(10500, "系统繁忙，请稍后再试"),

    SERVER_ERROR(10501, "服务器内部异常"),

    DATABASE_ERROR(10502, "数据库异常"),

    REDIS_ERROR(10503, "Redis服务异常"),

    UNKNOWN_ERROR(10504, "未知异常");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 返回消息
     */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}