package com.inventory.common.constants;

public class RedisConstants {

    /**
     * ===========================================
     * 登录态 AccessToken 前缀
     * 格式示例：
     *   user:token:{userId}:access:{accessToken}
     * 说明：
     *   - userId 用于多用户区分
     *   - accessToken 用于多设备支持
     * ===========================================
     */
    /**
     * 登录态 RefreshToken 前缀
     * 格式示例：
     *   user:token:{userId}:refresh:{refreshToken}
     * 说明：
     *   - userId 用于多用户区分
     *   - refreshToken 用于多设备支持
     * ===========================================
     */
    public static final String LOGIN_TOKEN_PREFIX = "user:token:";
    /**
     * 用户权限缓存前缀
     * 格式示例：
     *   user:perm:{userId}
     * 说明：
     *   - 只存用户当前权限列表
     *   - 用户权限变更 / 角色变更时清空
     * ===========================================
     */
    public static final String USER_PERMISSION_PREFIX = "user:perm:";

    /**
     * Redis 超级管理员权限标识
     * 可选：如果你在缓存里单独标记超级管理员
     */
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";


    /**
     * 用户设备映射
     *
     * user:device:{userId}
     *
     * Hash:
     * accessToken -> refreshToken
     */
    public static final String USER_DEVICE_PREFIX = "user:device:";

    /**
     * AI 客服多轮会话历史前缀（Redis 实现用）。
     * 完整 Key 示例：inventory:ai:chat:session:{sessionId}
     * TTL 由 inventory.ai.chat.session-ttl-seconds 控制。
     */
    public static final String AI_CHAT_SESSION_PREFIX = "inventory:ai:chat:session:";
}