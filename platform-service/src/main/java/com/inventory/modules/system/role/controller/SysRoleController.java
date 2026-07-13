package com.inventory.modules.system.role.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.common.response.Result;
import com.inventory.modules.system.role.entity.SysRole;
import com.inventory.modules.system.permission.vo.MenuVO;
import com.inventory.modules.system.permission.vo.SysRoleListVO;
import com.inventory.modules.system.role.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/sysRole")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;



    /**
     * 查询系统所有正常状态的角色列表
     * 通用接口，所有需要角色列表的地方都可以调用
     * 【注：开放接口，不加权限，用于用户选择角色】
     */
    @GetMapping("/listAll")
    public Result<List<SysRole>> listAll() {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getStatus, 1);
        return Result.success(sysRoleService.list(queryWrapper));
    }

    /**
     * 角色分页列表查询
     * @param keyword 关键词(角色名称/角色编码)
     * @param status 角色状态 0禁用 1正常
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 分页角色列表
     */
    @GetMapping("/list")
    @RequiresPerm("system:role:list")
    public Result<Page<SysRoleListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        // 调用业务层执行分页查询
        Page<SysRoleListVO> pageResult = sysRoleService.pageRole(keyword, status, pageNum, pageSize);
        return Result.success(pageResult);
    }

    // ==================== ↓↓↓ 【新增：根据ID查询单条角色】 ↓↓↓ ====================
    /**
     * 根据ID查询单条角色信息
     * 用于编辑回显
     */
    @GetMapping("/{id}")
    @RequiresPerm("system:role:list")
    public Result<SysRole> getById(@PathVariable String id) {
        return Result.success(sysRoleService.getById(id));
    }

    /**
     * 新增角色
     */
    @PostMapping
    @RequiresPerm("system:role:add")
    public Result<Void> add(@RequestBody SysRole role) {
        sysRoleService.save(role);
        return Result.success();
    }

    /**
     * 修改角色
     */
    @PutMapping("/{id}")
    @RequiresPerm("system:role:edit")
    public Result<Void> update(
            @PathVariable String id,
            @RequestBody SysRole role
    ) {
        role.setId(Long.valueOf(id));
        sysRoleService.updateById(role);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequiresPerm("system:role:delete")
    public Result<Void> delete(@PathVariable String id) {
        return sysRoleService.removeRoleById(id);
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    @RequiresPerm("system:role:batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return  sysRoleService.removeRoleByIds(ids);
    }

    /**
     * 修改角色状态（启用/禁用）
     */
    @PutMapping("/{id}/status")
    @RequiresPerm("system:role:changeStatus")
    public Result<Void> updateStatus(
            @PathVariable String id,
            @RequestParam Integer status
    ) {

        return sysRoleService.updateByRoleId(id,status);
    }

    /**
     * 获取系统所有权限的树形结构
     * 【角色分配权限专用】点击角色页面的"分配权限"按钮时调用
     * 返回所有目录、菜单、按钮，用于勾选分配
     */
    @GetMapping("/permission/tree")
    @RequiresPerm("system:role:assign")
    public Result<List<MenuVO>> getPermissionTree() {
        // 直接调用角色Service的方法，内部会复用权限Service的逻辑
        return Result.success(sysRoleService.getAllPermissionTree());
    }

    /**
     * 查询指定角色已拥有的权限ID列表
     * 用于分配权限弹窗打开时，自动回显已勾选的权限
     */
    @GetMapping("/{roleId}/permissionIds")
    @RequiresPerm("system:role:assign")
    public Result<List<Long>> getRolePermissionIds(@PathVariable Long roleId) {
        List<Long> rolePermissionIds = sysRoleService.getRolePermissionIds(roleId);
        return Result.success(rolePermissionIds);
    }

    /**
     * 保存角色的权限分配
     * 采用"先删后插"的逻辑，简单高效
     */
    @PostMapping("/{roleId}/permission")
    @RequiresPerm("system:role:assign")
    public Result<Void> saveRolePermission(
            @PathVariable Long roleId,
            @RequestBody List<Long> permIds
    ) {
        return sysRoleService.saveRolePermission(roleId, permIds);
    }

}