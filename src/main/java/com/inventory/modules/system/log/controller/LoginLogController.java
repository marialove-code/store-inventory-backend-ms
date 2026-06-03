package com.inventory.modules.system.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.common.response.Result;
import com.inventory.modules.system.log.entity.SysLoginLog;
import com.inventory.modules.system.log.service.SysLoginLogService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 登录日志控制器
 * 功能：登录日志列表查询、导出、清空
 *
 * @author yourname
 * @date 2026-05-25
 */
@RestController
@RequestMapping("/system/login/log")
@RequiredArgsConstructor
public class LoginLogController {

    private final SysLoginLogService loginLogService;

    /**
     * 登录日志分页列表查询
     * 接口地址：GET /system/login/log/list
     * 权限标识：system:loginlog:list
     *
     * @param userName     登录账号（模糊）
     * @param nickName     用户昵称（模糊）
     * @param loginIp      登录IP（模糊）
     * @param loginStatus  登录状态：1=成功，0=失败
     * @param beginTime    开始时间（YYYY-MM-DD HH:mm:ss）
     * @param endTime      结束时间（YYYY-MM-DD HH:mm:ss）
     * @param pageNum      页码，默认1
     * @param pageSize     每页条数，默认10
     * @return 分页结果
     */
    @GetMapping("/list")
    @RequiresPerm("system:loginlog:list")
    public Result<Page<SysLoginLog>> list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String loginIp,
            @RequestParam(required = false) Integer loginStatus,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Page<SysLoginLog> page = loginLogService.pageLoginLog(userName, nickName, loginIp, loginStatus,
                beginTime, endTime, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 导出登录日志为Excel
     * 接口地址：GET /system/login/log/export
     * 权限标识：system:loginlog:export
     *
     * @param response     响应对象（用于输出文件流）
     * @param userName     登录账号（模糊）
     * @param nickName     用户昵称（模糊）
     * @param loginIp      登录IP（模糊）
     * @param loginStatus  登录状态：1=成功，0=失败
     * @param beginTime    开始时间（YYYY-MM-DD HH:mm:ss）
     * @param endTime      结束时间（YYYY-MM-DD HH:mm:ss）
     * @throws IOException IO异常
     */
    @GetMapping("/export")
    @RequiresPerm("system:loginlog:export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String userName,
                       @RequestParam(required = false) String nickName,
                       @RequestParam(required = false) String loginIp,
                       @RequestParam(required = false) Integer loginStatus,
                       @RequestParam(required = false) String beginTime,
                       @RequestParam(required = false) String endTime) throws IOException {
        loginLogService.exportLoginLog(response, userName, nickName, loginIp, loginStatus, beginTime, endTime);
    }

    /**
     * 清空所有登录日志
     * 接口地址：DELETE /system/login/log/clear
     * 权限标识：system:loginlog:clear
     *
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    @RequiresPerm("system:loginlog:clear")
    public Result<Void> clear() {
        loginLogService.clearLoginLog();
        return Result.success();
    }
}