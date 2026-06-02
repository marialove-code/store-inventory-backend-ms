package com.inventory.modules.monitor.apimonitor.vo;

import lombok.Data;

@Data
public class ApiRequestTrendVo {

    private String time;

    private Long requestCount;
}