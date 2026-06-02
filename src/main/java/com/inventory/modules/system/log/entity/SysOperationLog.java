package com.inventory.modules.system.log.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 系统操作审计日志表
 * @TableName sys_operation_log
 */
@TableName(value ="sys_operation_log")
@Data
public class SysOperationLog {
    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 操作人账号
     */
    private String username;

    /**
     * 操作模块
     */
    private String title;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求接口
     */
    private String requestUri;

    /**
     * 客户端IP
     */
    private String ipAddress;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 状态 1成功 0失败
     */
    private Integer operateStatus;

    /**
     * 异常信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}