package com.inventory.modules.monitor.apimonitor.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.monitor.apimonitor.query.ApiMonitorQuery;
import com.inventory.modules.monitor.apimonitor.service.SysApiMonitorService;
import com.inventory.modules.monitor.apimonitor.vo.ApiMonitorVo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class ApiMonitorController {

    private final SysApiMonitorService apiMonitorService;

    @GetMapping("/api")
    @PreAuthorize("hasAuthority('monitor:api:list')")
    public Result<ApiMonitorVo> getApiMonitor(
            ApiMonitorQuery query) {

        return Result.success(
                apiMonitorService.getApiMonitor(query)
        );
    }
}