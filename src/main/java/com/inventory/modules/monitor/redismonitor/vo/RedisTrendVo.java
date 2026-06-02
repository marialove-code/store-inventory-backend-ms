package com.inventory.modules.monitor.redismonitor.vo;

import com.inventory.modules.monitor.redismonitor.entity.TrendItem;
import lombok.Data;

import java.util.List;

@Data
public class RedisTrendVo {

    /**
     * 内存趋势
     */
    private List<TrendItem> memoryUsageTrend;

    /**
     * 命中率趋势
     */
    private List<TrendItem> hitRateTrend;

    /**
     * QPS趋势
     */
    private List<TrendItem> commandQpsTrend;
}