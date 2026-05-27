package com.inventory.modules.system.online.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 在线用户实体（从Redis读取）
 */
@Data
public class OnlineUser {

    /**
     * Redis 的 key（login:token:xxx）
     */
    private String tokenKey;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 登录IP
     */
    private String ipaddr;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime loginTime;

    /**
     * 过期时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime expireTime;
}