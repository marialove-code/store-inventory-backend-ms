package com.inventory.modules.own.home.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 安全信息VO
 * 登录信息来自 SysLoginLog
 * 在线设备来自 Redis
 */
@Data
public class ProfileSecurityInfoVO {
    private Integer onlineDeviceCount; // 在线设备数量（Redis）
    private String lastLoginIp;        // 最后登录IP（日志）
    private LocalDateTime lastLoginTime; // 最后登录时间（日志）
    private String lastLoginAddress;   // 最后登录地点（日志）
    private String tokenStatus;        // 当前Token状态
}