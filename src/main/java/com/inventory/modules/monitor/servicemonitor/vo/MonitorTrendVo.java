package com.inventory.modules.monitor.servicemonitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监控趋势数据
 *
 * 用于：
 * Heap趋势图
 * NonHeap趋势图
 * GC趋势图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "监控趋势数据")
public class MonitorTrendVo {

    /**
     * 时间点
     *
     * 示例：
     * 10:00
     * 11:00
     * 12:00
     */
    @Schema(description = "时间点")
    private String time;

    /**
     * 当前指标值
     *
     * Heap -> MB
     * NonHeap -> MB
     * GC -> 次数
     */
    @Schema(description = "指标值")
    private Double value;

}