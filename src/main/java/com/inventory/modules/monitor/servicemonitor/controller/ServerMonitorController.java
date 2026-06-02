package com.inventory.modules.monitor.servicemonitor.controller;
import com.inventory.common.response.Result;
import com.inventory.modules.monitor.servicemonitor.service.ServerMonitorService;
import com.inventory.modules.monitor.servicemonitor.vo.ServerMonitorVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务监控
 */
@RestController
@RequestMapping("/monitor/server")
@RequiredArgsConstructor
public class ServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    /**
     * 获取服务监控信息
     */
    @GetMapping
    public Result<ServerMonitorVo> getInfo() {

        return Result.success(
                serverMonitorService.getInfo()
        );
    }

}