package com.inventory.service;

import com.inventory.entity.SysPermission;
import com.inventory.entity.SysRolePermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role_permission(角色权限关联表)】的数据库操作Service
* @createDate 2026-05-09 17:31:43
*/
public interface SysRolePermissionService extends IService<SysRolePermission> {




    /**
     * 根据用户ID查询【用户拥有的菜单/权限列表】
     * 只查询 M（目录）、C（菜单）类型，用于前端侧边栏渲染
     *
     * @param userId 用户ID
     * @return 菜单/权限集合
     */
    List<SysPermission> listUserPermissionsByUserId(Long userId);
}
