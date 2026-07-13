package com.inventory.common.constants;

/**
 * Redis Key 前缀常量（各微服务共享同一套约定）。
 * <p>
 * platform-service 登录/踢下线写入；后续 Gateway 或其它服务若校验登录态，
 * 必须使用相同前缀与 Redis 实例，否则会话无法互通。
 * </p>
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /**
     * 登录态 Token 前缀。
     * <p>
     * Access 示例：{@code user:token:{userId}:access:{accessToken}}<br>
     * Refresh 示例：{@code user:token:{userId}:refresh:{refreshToken}}
     * </p>
     */
    public static final String LOGIN_TOKEN_PREFIX = "user:token:";

    /**
     * 用户权限缓存前缀：{@code user:perm:{userId}}。
     * 角色/权限变更或踢下线时应删除，避免旧权限继续生效。
     */
    public static final String USER_PERMISSION_PREFIX = "user:perm:";

    /**
     * 超级管理员角色标识（缓存或业务判断用）。
     */
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    /**
     * 用户设备映射：{@code user:device:{userId}}（Hash：accessToken → refreshToken）。
     * 用于多端登录管理与踢下线。
     */
    public static final String USER_DEVICE_PREFIX = "user:device:";

    /**
     * AI 客服多轮会话历史前缀（ai-service Redis 实现用）。
     * 完整 Key：{@code inventory:ai:chat:session:{sessionId}}。
     */
    public static final String AI_CHAT_SESSION_PREFIX = "inventory:ai:chat:session:";
}
