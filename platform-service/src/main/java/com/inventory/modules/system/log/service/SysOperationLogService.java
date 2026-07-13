package com.inventory.modules.system.log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
* @author 95349
* @description 针对表【sys_operation_log(系统操作审计日志表)】的数据库操作Service
* @createDate 2026-05-08 10:31:31
*/
public interface SysOperationLogService extends IService<SysOperationLog> {


    /**
     * 分页查询操作日志
     */
    Page<SysOperationLog> pageList(String username, String title, String operationType,
                                   Short operateStatus, String beginTime, String endTime,
                                   Long pageNum, Long pageSize);

    /**
     * 导出Excel
     */
    void exportExcel(HttpServletResponse response, String username, String title, String operationType,
                     Short operateStatus, String beginTime, String endTime) throws IOException;



    /**
     * 【个人】查询我的操作日志
     */
    Page<SysOperationLog> pageListMy(String title, String operationType,
                                     Short operateStatus, String beginTime, String endTime,
                                     Long pageNum, Long pageSize);

    /**
     * 【个人】导出我的操作日志
     */
    void exportExcelMy(HttpServletResponse response, String title, String operationType,
                       Short operateStatus, String beginTime, String endTime) throws IOException;
}
