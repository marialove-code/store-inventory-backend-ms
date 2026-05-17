package com.inventory.controller;

import com.inventory.common.result.Result;
import com.inventory.context.LoginUserContext;
import com.inventory.entity.SysPermission;
import com.inventory.entity.menu.MenuVO;
import com.inventory.service.SysPermissionService;
import com.inventory.service.SysRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private SysRolePermissionService rolePermissionService;



    @GetMapping("/all")
    public List<MenuVO> getAllMenus() {
        List<SysPermission> list = permissionService.lambdaQuery()
                .in(SysPermission::getPermType, "M", "C")
                .list();
        return permissionService.buildMenuTree(list);
    }

    /**
     * 获取当前用户的菜单树（用于前端渲染侧边栏）
     */
    @GetMapping("/tree")
    public Result<List<MenuVO>> getCurrentUserMenuTree() {

        // 1. 获取当前登录用户ID（从SecurityContext里拿）
        Long userId = LoginUserContext.getUserId();
        
        // 2. 根据用户ID查询菜单列表（只查目录M和菜单C，不查按钮F）
        List<SysPermission> permissions = rolePermissionService.listUserPermissionsByUserId(userId);
        
        // 3. 构建树形结构（递归组装children）
        List<MenuVO> menuTree = permissionService.buildMenuTree(permissions);
        
        return Result.success(menuTree);
    }
}