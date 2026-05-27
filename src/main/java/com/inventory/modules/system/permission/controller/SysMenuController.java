package com.inventory.modules.system.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.enums.OperationTypeEnum;
import com.inventory.common.response.Result;
import com.inventory.framework.log.annotation.OperationLog;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.system.permission.entity.SysPermission;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.system.permission.vo.MenuVO;
import com.inventory.modules.system.permission.service.SysPermissionService;
import com.inventory.modules.system.permission.service.SysRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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

        // 1. 获取当前登录用户
        LoginUserVO loginUser = LoginUserContext.getUser();
        List<String> roles = loginUser.getRoles();
        List<SysPermission> menuList;
        // 2. 判断是否是超级管理员
        boolean isSuperAdmin = roles.contains("SUPER_ADMIN");
        if (isSuperAdmin) {
            // 超级管理员 → 查询所有菜单和按钮
            menuList = permissionService.lambdaQuery()
                    .in(SysPermission::getPermType, "M", "C")
                    .list();
        } else {
            // 普通用户 → 只查询当前用户拥有权限的菜单
            menuList = permissionService.getMenuPermissionsByUserId(loginUser.getUserId());
        }

        // 3. 构建树形菜单
        List<MenuVO> tree = permissionService.buildMenuTree(menuList);
        return tree;
    }

    /**
     * 菜单列表查询（只返回目录和菜单，支持关键词搜索，返回树形结构）
     */
    @GetMapping("/list")
    @RequiresPerm("system:menu:list")
    public Result<List<MenuVO>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        // 只查询 目录(M) 和 菜单(C)，过滤按钮(F)
        wrapper.in(SysPermission::getPermType, "M", "C");
        // 模糊查询菜单名称
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysPermission::getPermName, keyword);
        }
        // 按排序号升序
        wrapper.orderByAsc(SysPermission::getSort);

        List<SysPermission> menuList = permissionService.list(wrapper);
        // 构建树形结构（复用你已有的 buildMenuTree 方法）
        List<MenuVO> menuTree = permissionService.buildMenuTree(menuList);
        return Result.success(menuTree);
    }

    /**
     * 新增菜单（只允许新增目录/菜单类型）
     */
    @PostMapping
    @RequiresPerm("system:menu:add")
    public Result<Void> add(@RequestBody SysPermission menu) {
        // 强制校验：只允许目录或菜单类型
        String type = menu.getPermType();
        if (!"M".equals(type) && !"C".equals(type)) {
            return Result.fail("菜单类型只能为目录或菜单");
        }
        permissionService.save(menu);
        return Result.success();
    }

    /**
     * 修改菜单
     */
    @PutMapping("/{id}")
    @RequiresPerm("system:menu:edit")
    public Result<Void> update(@PathVariable String id, @RequestBody SysPermission menu) {
        // 强制校验：只允许目录或菜单类型
        String type = menu.getPermType();
        if (!"M".equals(type) && !"C".equals(type)) {
            return Result.fail("菜单类型只能为目录或菜单");
        }
        menu.setId(Long.valueOf(id));
        permissionService.updateById(menu);
        return Result.success();
    }

    /**
     * 删除菜单（级联校验：有子菜单时禁止删除）
     */
    @DeleteMapping("/{id}")
    @RequiresPerm("system:menu:delete")
    public Result<Void> delete(@PathVariable String id) {
        return permissionService.removeMenuById(id);

    }

    /**
     * 修改菜单状态
     */
    @PutMapping("/{id}/status")
    @RequiresPerm("system:menu:changeStatus")
    @OperationLog(title = "修改菜单状态", type = OperationTypeEnum.UPDATE)
    public Result<Void> updateMenuStatus(@PathVariable String id, @RequestParam Integer status) {
        return permissionService.updateMenuStatus(id, status);
    }
}