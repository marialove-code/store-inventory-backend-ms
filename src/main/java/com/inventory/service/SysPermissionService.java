package com.inventory.service;

import com.inventory.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Service
* @createDate 2026-05-09 17:31:43
*/
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据用户ID查询权限标识列表
     */
    List<String> listPermCodesByUserId(Long userId);

}
