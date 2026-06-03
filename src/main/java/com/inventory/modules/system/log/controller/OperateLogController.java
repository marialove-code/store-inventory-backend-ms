package com.inventory.modules.system.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.response.Result;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.inventory.modules.system.log.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/system/operate/log")
@RequiredArgsConstructor
public class OperateLogController {

    private final SysOperationLogService operateLogService;

    /**
     * 分页列表
     */
    @GetMapping("/list")
    @RequiresPerm("system:operlog:list")
    public Result<Page<SysOperationLog>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Short operateStatus,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Page<SysOperationLog> page = operateLogService.pageList(username, title, operationType,
                operateStatus, beginTime, endTime, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 导出Excel
     */
    @GetMapping("/export")
    @RequiresPerm("system:operlog:export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String title,
                       @RequestParam(required = false) String operationType,
                       @RequestParam(required = false) Short operateStatus,
                       @RequestParam(required = false) String beginTime,
                       @RequestParam(required = false) String endTime) throws IOException {
        operateLogService.exportExcel(response, username, title, operationType, operateStatus, beginTime, endTime);
    }
}
