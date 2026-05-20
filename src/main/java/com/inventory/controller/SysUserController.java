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
        sysUserService.saveUserRole(userId, roleIds);
        return Result.success();
    }



    // 头像上传接口（前端直接调用的 /sysUser/uploadAvatar）
    @PostMapping("/uploadAvatar")
    public Result<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        // 1. 判空
        if (file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        // 2. 格式校验
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!suffix.matches(".(png|jpg|jpeg|gif)")) {
            return Result.fail("只支持png、jpg、jpeg、gif格式");
        }

        // 3. 大小校验（2MB = 2 * 1024 * 1024）
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.fail("图片大小不能超过2MB");
        }

        // 4. 生成唯一文件名，防止覆盖
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        try {
            // 5. 存储目录（你可以改成自己的路径）
            String uploadPath = "E:/Image/upload/avatar/";
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            // 6. 保存文件
            file.transferTo(new File(uploadPath + fileName));

            // 7. 返回可访问的头像URL（关键！前端需要这个）
            String avatarUrl = "/upload/avatar/" + fileName;
            return Result.success(avatarUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("头像上传失败");
        }
    }
}