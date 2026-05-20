package com.inventory.service;

import com.inventory.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.entity.menu.MenuVO;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Service
* @createDate 2026-05-09 17:31:43
*/
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据用户ID查询【权限标识perm_code】列表
     * 用于 Spring Security 权限校验，如：system:user:list
     *
     * @param userId 用户ID
     * @return 权限标识字符串集合
     */
    List<String> listPermCodesByUserId(Long userId);

    /**
     * 构建【前端树形菜单】
     * 把平铺的权限列表数据，根据 parent_id 递归组装成树形结构
     *
     * @param permissions 权限/菜单列表
     * @return 树形结构菜单数据
     */
    List<MenuVO> buildMenuTree(List<SysPermission> permissions);


    /**
     * 构建【前端树形菜单】
     * 把平铺的权限列表数据，根据 parent_id 递归组装成树形结构
     *
     * @param permissions 权限/菜单列表
     * @return 树形结构菜单数据
     */
    List<MenuVO> buildAllMenuTree(List<SysPermission> permissions);

    /**
     * 查询系统所有正常状态的权限
     * 包含：目录M、菜单C、按钮F
     */
    List<SysPermission> listAllNormalPermissions();

    /**
     * 获取全部权限 （超级管路员）
     * @return
     */
    List<String> listAllPermCodes();

    /**
     * 根据用户ID获取菜单
     * @param userId
     * @return
     */
    List<SysPermission> getMenuPermissionsByUserId(Long userId);
}
