package com.inventory.modules.monitor.redismonitor.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.monitor.redismonitor.service.RedisMonitorService;
import com.inventory.modules.monitor.redismonitor.vo.RedisMonitorVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis监控
 *
 * 只查询实时基础指标
 *
 * 不查询趋势数据
 * 不扫描大Key
 *
 * 保证接口响应速度
 */
@RestController
@RequestMapping("/monitor/redis")
@RequiredArgsConstructor
public class RedisMonitorController {

    private final RedisMonitorService redisMonitorService;

    /**
     * Redis基础监控信息
     */
    @GetMapping("/info")
    @RequiresPerm("monitor:redis:view")
    public Result<RedisMonitorVo> getInfo() {

        return Result.success(
                redisMonitorService.getInfo()
        );
    }
}