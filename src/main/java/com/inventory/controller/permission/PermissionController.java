package com.inventory.controller.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.annotation.RequiresPerm;
import com.inventory.common.result.Result;
import com.inventory.entity.SysPermission;
import com.inventory.entity.menu.MenuVO;
import com.inventory.service.SysPermissionService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sysPermission")
public class PermissionController {

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 权限分页列表查询（适配前端：名称、类型、状态筛选）
     * @param keyword 关键词(权限名称)
     * @param permType 权限类型 M/目录 C/菜单 F/按钮
     * @param status 权限状态 0禁用 1正常
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 分页权限列表
     */
    @GetMapping("/list")
    @RequiresPerm("system:permission:list")
    public Result<Page<SysPermission>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String permType,
            @RequestParam(required = false) Integer status
    ) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();

        // 模糊查询：权限名称
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysPermission::getPermName, keyword);
        }
        // 按类型筛选
        if (StringUtils.hasText(permType)) {
            wrapper.eq(SysPermission::getPermType, permType);
        }
        // 按状态筛选
        if (status != null) {
            wrapper.eq(SysPermission::getStatus, status);
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
    @RequiresPerm("system:permission:list")
    public Result<List<MenuVO>> getPermissionTree() {
        // 1. 查询数据库中【所有】权限数据
        List<SysPermission> allPermissionList = sysPermissionService.list();

        // 2. 把权限列表 转换成 树形结构
        List<MenuVO> permissionTree = sysPermissionService.buildAllMenuTree(allPermissionList);

        // 3. 返回给前端
        return Result.success(permissionTree);
    }

    // ==================== 以下为新增/优化的增删改查接口 ====================

    /**
     * 根据ID查询单条权限信息（用于编辑回显）
     */
    @GetMapping("/{id}")
    @RequiresPerm("system:permission:list")
    public Result<SysPermission> getById(@PathVariable Long id) {
        return Result.success(sysPermissionService.getById(id));
    }

    /**
     * 新增权限
     */
    @PostMapping
    @RequiresPerm("system:permission:add")
    public Result<Void> add(@RequestBody SysPermission permission) {
        sysPermissionService.save(permission);
        return Result.success();
    }

    /**
     * 修改权限
     */
    @PutMapping("/{id}")
    @RequiresPerm("system:permission:edit")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody SysPermission permission
    ) {
        permission.setId(id);
        sysPermissionService.updateById(permission);
        return Result.success();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @RequiresPerm("system:permission:delete")
    public Result<Void> delete(@PathVariable Long id) {
        sysPermissionService.removeById(id);
        return Result.success();
    }

    /**
     * 批量删除权限（可选，和角色管理保持一致）
     */
    @DeleteMapping("/batch")
    @RequiresPerm("system:permission:batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        sysPermissionService.removeByIds(ids);
        return Result.success();
    }

    /**
     * 修改权限状态（启用/禁用）
     */
    @PutMapping("/{id}/status")
    @RequiresPerm("system:permission:changeStatus")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setStatus(status);
        sysPermissionService.updateById(permission);
        return Result.success();
    }

    /**
     * 查询所有权限标识（给超级管理员使用，返回所有 permCode）
     */
    @GetMapping("/listAllPermCodes")
    public Result<List<String>> listAllPermCodes() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, 1);
        wrapper.select(SysPermission::getPermCode);
        List<SysPermission> list = sysPermissionService.list(wrapper);
        List<String> permCodes = list.stream()
                .map(SysPermission::getPermCode)
                .filter(StringUtils::hasText)
                .toList();
        return Result.success(permCodes);
    }

}