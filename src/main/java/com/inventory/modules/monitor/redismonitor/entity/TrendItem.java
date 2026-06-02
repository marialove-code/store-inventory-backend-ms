package com.inventory.modules.monitor.redismonitor.entity;

import lombok.Data;

/**
 * 趋势图数据
 */
@Data
public class TrendItem {

    /**
     * 时间
     *
     * 示例：
     * 14:30
     */
    private String time;

    /**
     * 值
     */
    private Double value;
}