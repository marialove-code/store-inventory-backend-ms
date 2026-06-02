package com.inventory.modules.monitor.redismonitor.service;

import com.inventory.modules.monitor.redismonitor.vo.RedisTrendVo;

public interface RedisTrendService {

    /**
     * 获取趋势图
     */
    RedisTrendVo getTrend();
}