package com.inventory.modules.monitor.apimonitor.query;

import lombok.Data;

@Data
public class ApiMonitorQuery {

    /**
     * 接口路径
     */
    private String apiPath;

    /**
     * NORMAL/SLOW/ERROR
     */
    private String status;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}