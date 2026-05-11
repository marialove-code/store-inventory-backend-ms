package com.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.annotation.OperationLog;
import com.inventory.annotation.RateLimit;
import com.inventory.annotation.RequiresPerm;
import com.inventory.common.enums.OperationTypeEnum;
import com.inventory.common.result.Result;
import com.inventory.entity.*;
import com.inventory.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器（工程化整改）
 * 只负责：接口接收 + 调用Service + 返回结果
 * 无任何业务逻辑、无Redis、无密码加密
 */
@RestController
@RequestMapping("/sysUser")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    // ============================ 用户管理 =============================

    @GetMapping("/list")
    @RequiresPerm("system:user:list")
    public Result<Page<SysUserListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return Result.success(sysUserService.pageUser(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/user/{id}")
    @RequiresPerm("system:user:list")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        return Result.success(sysUserService.getUserById(id));
    }

    @PutMapping("/user/{id}")
    @OperationLog(title = "修改用户", type = OperationTypeEnum.UPDATE)
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody SysUser dto) {
        sysUserService.updateUser(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequiresPerm("system:user:changeStatus")
    @OperationLog(title = "修改状态", type = OperationTypeEnum.UPDATE)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/resetPassword")
    @RequiresPerm("system:user:resetPwd")
    @OperationLog(title = "重置密码", type = OperationTypeEnum.UPDATE)
    public Result<Void> resetPwd(@PathVariable Long id) {
        sysUserService.resetPassword(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPerm("system:user:delete")
    @OperationLog(title = "删除用户", type = OperationTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @RequiresPerm("system:user:delete")
    @OperationLog(title = "批量删除", type = OperationTypeEnum.DELETE)
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        sysUserService.batchDelete(ids);
        return Result.success();
    }
}