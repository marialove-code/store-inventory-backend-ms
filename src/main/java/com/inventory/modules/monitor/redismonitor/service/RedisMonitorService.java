package com.inventory.modules.monitor.redismonitor.service;

import com.inventory.modules.monitor.redismonitor.vo.RedisMonitorVo;

public interface RedisMonitorService {

    /**
     * 获取Redis基础监控信息
     */
    RedisMonitorVo getInfo();
}