package com.inventory.modules.monitor.apimonitor.vo;

import lombok.Data;

import java.util.List;

@Data
public class ApiMonitorVo {

    private ApiMonitorSummaryVo summary;

    private List<ApiPerfItemVo> apiList;

    private List<ApiRequestTrendVo> requestTrend;
}