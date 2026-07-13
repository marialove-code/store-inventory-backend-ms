package com.inventory.modules.monitor.apimonitor.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_api_monitor")
public class SysApiMonitor {

    @TableId
    private Long id;

    /**
     * 接口路径
     */
    private String apiPath;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 响应耗时(ms)
     */
    private Long responseTime;

    /**
     * 是否成功
     */
    private Integer successFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}