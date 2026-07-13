package com.inventory.modules.system.log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.modules.system.log.entity.SysLoginLog;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface SysLoginLogService extends IService<SysLoginLog> {


    /**
     * 分页查询登录日志
     *
     * @param userName     登录账号（模糊）
     * @param nickName     用户昵称（模糊）
     * @param loginIp      登录IP（模糊）
     * @param loginStatus  登录状态：1=成功，0=失败
     * @param beginTime    开始时间（YYYY-MM-DD HH:mm:ss）
     * @param endTime      结束时间（YYYY-MM-DD HH:mm:ss）
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @return 分页结果
     */
    Page<SysLoginLog> pageLoginLog(String userName, String nickName, String loginIp, Integer loginStatus,
                                   String beginTime, String endTime, Long pageNum, Long pageSize);

    /**
     * 导出登录日志为Excel
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
    void exportLoginLog(HttpServletResponse response, String userName, String nickName, String loginIp,
                        Integer loginStatus, String beginTime, String endTime) throws IOException;

    /**
     * 清空所有登录日志
     */
    void clearLoginLog();

    /**
     * 【个人】查询我的登录日志（只查自己）
     */
    Page<SysLoginLog> pageLoginLogMy(String loginIp, Integer loginStatus,
                                     String beginTime, String endTime,
                                     Long pageNum, Long pageSize);

    /**
     * 【个人】导出我的登录日志（只导出自己）
     */
    void exportLoginLogMy(HttpServletResponse response, String loginIp, Integer loginStatus,
                          String beginTime, String endTime) throws IOException;
}