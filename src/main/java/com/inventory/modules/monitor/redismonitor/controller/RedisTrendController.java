package com.inventory.modules.monitor.redismonitor.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.monitor.redismonitor.service.RedisTrendService;
import com.inventory.modules.monitor.redismonitor.task.RedisTrendCollectTask;
import com.inventory.modules.monitor.redismonitor.vo.RedisTrendVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/redis")
@RequiredArgsConstructor
public class RedisTrendController {

    private final RedisTrendService trendService;


    private final RedisTrendCollectTask trendTask;

    /**
     * Redis趋势图
     */
    @GetMapping("/trend")
    public Result<RedisTrendVo> trend() {

        return Result.success(
                trendService.getTrend()
        );
    }


  /*  *//**
     * 手动触发趋势图采集
     *//*
    @GetMapping("/collect")
    public String triggerTrend() {
        trendTask.collectOnce();
        return "趋势图缓存已更新";
    }*/
}