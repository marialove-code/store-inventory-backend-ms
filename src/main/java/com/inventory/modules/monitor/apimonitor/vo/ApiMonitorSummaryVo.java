package com.inventory.modules.monitor.apimonitor.vo;

import lombok.Data;

@Data
public class ApiMonitorSummaryVo {

    private Long totalRequests;

    private Long successRequests;

    private Long failRequests;

    private Double avgResponseTime;
}