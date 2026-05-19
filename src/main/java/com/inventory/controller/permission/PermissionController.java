package com.inventory.controller.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.Result;
import com.inventory.entity.SysPermission;
import com.inventory.entity.menu.MenuVO;
import com.inventory.service.SysPermissionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sysPermission")
public class PermissionController {

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 权限分页列表（给前端权限管理页面用）
     */
    @GetMapping("/list")
    public Result<Page<SysPermission>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();

        // 模糊查询：权限名称
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysPermission::getPermName, keyword);
        }

        // 排序：sort 正序
        wrapper.orderByAsc(SysPermission::getSort);

        Page<SysPermission> page = sysPermissionService.page(
                new Page<>(pageNum, pageSize),
                wrapper
        );

        return Result.success(page);
    }


    /**
     * 【权限树接口】
     * 作用：给前端【新增/编辑权限】的【上级权限】下拉框使用
     * 返回：层级结构的权限树（目录 → 菜单 → 按钮）
     */
    @GetMapping("/tree")
    public Result<List<MenuVO>> getPermissionTree() {
        // 1. 查询数据库中【所有】权限数据
        List<SysPermission> allPermissionList = sysPermissionService.list();

        // 2. 把权限列表 转换成 树形结构
        List<MenuVO> permissionTree = sysPermissionService.buildAllMenuTree(allPermissionList);

        // 3. 返回给前端
        return Result.success(permissionTree);
    }
}