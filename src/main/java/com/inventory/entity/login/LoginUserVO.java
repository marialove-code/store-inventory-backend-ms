package com.inventory.entity.login;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录用户信息
 *
 * 存入 Redis
 * 用于权限校验
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录用户名
     */
    private String username;


    /**
     * 昵称
     */
    private String nickName;

    /**
     * 用户角色
     */
    private List<String> roles;

    /**
     * 用户权限
     */
    private List<String> permissions;

    /**
     * 是否超级管理员
     */
    private Boolean admin;

    private String ipaddr;        // 登录IP
    private String browser;       // 浏览器
    private String os;            // 操作系统
    private LocalDateTime loginTime;  // 登录时间
    private LocalDateTime expireTime; // 过期时间
}