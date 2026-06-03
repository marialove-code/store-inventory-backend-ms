package com.inventory.modules.own.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.response.Result;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.system.log.entity.SysLoginLog;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.inventory.modules.system.log.service.SysLoginLogService;
import com.inventory.modules.system.log.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 登录日志控制器
 * 功能：登录日志列表查询、导出、清空
 *
 * @author yourname
 * @date 2026-05-25
 */
@RestController
@RequestMapping("/system/login/mylog")
@RequiredArgsConstructor
public class LogController {

    private final SysLoginLogService loginLogService;

    private final SysOperationLogService operateLogService;

    /**
     * 登录日志分页列表查询
     * 接口地址：GET /system/login/log/list
     * 权限标识：system:loginlog:list
     *
     * @param loginIp      登录IP（模糊）
     * @param loginStatus  登录状态：1=成功，0=失败
     * @param beginTime    开始时间（YYYY-MM-DD HH:mm:ss）
     * @param endTime      结束时间（YYYY-MM-DD HH:mm:ss）
     * @param pageNum      页码，默认1
     * @param pageSize     每页条数，默认10
     * @return 分页结果
     */
    @GetMapping("/loglist")
    public Result<Page<SysLoginLog>> list(
            @RequestParam(required = false) String loginIp,
            @RequestParam(required = false) Integer loginStatus,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Page<SysLoginLog> page = loginLogService.pageLoginLogMy(loginIp, loginStatus,
                beginTime, endTime, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 导出登录日志为Excel
     * 接口地址：GET /system/login/log/export
     * 权限标识：system:loginlog:export
     *
     * @param response     响应对象（用于输出文件流）
     * @param loginIp      登录IP（模糊）
     * @param loginStatus  登录状态：1=成功，0=失败
     * @param beginTime    开始时间（YYYY-MM-DD HH:mm:ss）
     * @param endTime      结束时间（YYYY-MM-DD HH:mm:ss）
     * @throws IOException IO异常
     */
    @GetMapping("/logExport")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String loginIp,
                       @RequestParam(required = false) Integer loginStatus,
                       @RequestParam(required = false) String beginTime,
                       @RequestParam(required = false) String endTime) throws IOException {
        loginLogService.exportLoginLogMy(response,  loginIp, loginStatus, beginTime, endTime);
    }


    /**
     * 分页列表
     */
    @GetMapping("/operList")
    public Result<Page<SysOperationLog>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Short operateStatus,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Page<SysOperationLog> page = operateLogService.pageListMy( title, operationType,
                operateStatus, beginTime, endTime, pageNum, pageSize);
        return Result.success(page);
    }


    /**
     * 导出Excel
     */
    @GetMapping("/operExport")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String title,
                       @RequestParam(required = false) String operationType,
                       @RequestParam(required = false) Short operateStatus,
                       @RequestParam(required = false) String beginTime,
                       @RequestParam(required = false) String endTime) throws IOException {
        operateLogService.exportExcelMy(response, title, operationType, operateStatus, beginTime, endTime);
    }

}