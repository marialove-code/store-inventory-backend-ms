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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
    public Result<SysUser> getUserById(@PathVariable String id) {
        return Result.success(sysUserService.getUserById(id));
    }

    @PutMapping("/user/{id}")
    @OperationLog(title = "修改用户", type = OperationTypeEnum.UPDATE)
    public Result<Void> updateUser(@PathVariable String id, @RequestBody SysUser dto) {
        sysUserService.updateUser(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequiresPerm("system:user:changeStatus")
    @OperationLog(title = "修改状态", type = OperationTypeEnum.UPDATE)
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        return sysUserService.updateUserStatus(id, status);
    }

    @PutMapping("/{id}/resetPassword")
    @RequiresPerm("system:user:resetPwd")
    @OperationLog(title = "重置密码", type = OperationTypeEnum.UPDATE)
    public Result<Void> resetPwd(@PathVariable String id) {
        sysUserService.resetPassword(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPerm("system:user:delete")
    @OperationLog(title = "删除用户", type = OperationTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable String id) {
        return sysUserService.removeUserById(id);
    }

    @DeleteMapping("/batch")
    @RequiresPerm("system:user:delete")
    @OperationLog(title = "批量删除", type = OperationTypeEnum.DELETE)
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return sysUserService.batchRemoveUser(ids);

    }

    /**
     * 查询指定用户已拥有的角色ID列表
     * 用于分配角色弹窗的回显
     */
    @GetMapping("/{userId}/roleIds")
    public Result<List<Long>> getUserRoleIds(@PathVariable Long userId) {
        return Result.success(sysUserService.getUserRoleIds(userId));
    }

    /**
     * 保存用户的角色分配
     */
    @PostMapping("/{userId}/role")
    public Result<Void> saveUserRole(
            @PathVariable Long userId,
            @RequestBody List<Long> roleIds
    ) {
        return sysUserService.assignUserRole(userId, roleIds);
    }



    // 1. 接口里注入配置文件的路径
    @PostMapping("/uploadAvatar")
    public Result<?> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            @Value("${app.upload.avatar-path}") String uploadPath
    ) {
        if (file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        // 修复正则表达式的语法错误
        if (!suffix.matches("\\.(png|jpg|jpeg|gif)")) {
            return Result.fail("只支持png、jpg、jpeg、gif格式");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail("图片大小不能超过2MB");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            file.transferTo(new File(dir, fileName));

            String avatarUrl = "/upload/avatar/" + fileName;
            return Result.success(avatarUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("头像上传失败");
        }
    }
}