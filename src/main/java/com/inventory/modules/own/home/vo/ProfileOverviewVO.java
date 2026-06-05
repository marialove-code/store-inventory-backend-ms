package com.inventory.modules.own.home.vo;

import com.inventory.modules.system.log.entity.SysLoginLog;
import lombok.Data;
import java.util.List;

/**
 * 个人主页总览VO
 * 前端一次性获取所有个人中心数据
 */
@Data
public class ProfileOverviewVO {
    private ProfileBasicInfoVO basicInfo;       // 基础信息（来自SysUser）
    private ProfileSecurityInfoVO securityInfo; // 安全信息（登录日志+Redis）
    private ProfilePermissionInfoVO permissionInfo; // 权限信息
    private List<SysLoginLog> recentLoginLogs;  // 最近3条登录日志
}