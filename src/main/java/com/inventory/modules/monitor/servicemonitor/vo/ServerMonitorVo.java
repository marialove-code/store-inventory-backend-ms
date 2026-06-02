package com.inventory.modules.monitor.servicemonitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 服务监控返回对象
 */
@Data
@Schema(description = "服务监控信息")
public class ServerMonitorVo {

    /** 服务名称 */
    @Schema(description = "Service name")
    private String serviceName;

    /** 服务状态 UP/DOWN/RUNNING */
    @Schema(description = "Service status")
    private String status;

    /** JVM 版本 */
    @Schema(description = "JVM version")
    private String jvmVersion;

    /** JDK 版本 */
    @Schema(description = "JDK version")
    private String jdkVersion;

    /** Heap 已使用内存(MB) */
    @Schema(description = "Heap used memory(MB)")
    private Double heapUsed;

    /** NonHeap 已使用内存(MB) */
    @Schema(description = "NonHeap used memory(MB)")
    private Double nonHeapUsed;

    /** GC 总次数 */
    @Schema(description = "GC total count")
    private Long gcCount;

    /** Heap 使用趋势 */
    @Schema(description = "Heap usage trend")
    private List<MonitorTrendVo> heapUsedTrend;

    /** NonHeap 使用趋势 */
    @Schema(description = "NonHeap usage trend")
    private List<MonitorTrendVo> nonHeapUsedTrend;

    /** GC 次数趋势 */
    @Schema(description = "GC count trend")
    private List<MonitorTrendVo> gcCountTrend;

    /** 当前线程数 */
    @Schema(description = "Current thread count")
    private Integer threadCount;

    /** 峰值线程数 */
    @Schema(description = "Peak thread count")
    private Integer peakThreadCount;

    /** JVM 启动时间 */
    @Schema(description = "JVM start time")
    private String jvmStartTime;

    /** 服务运行时长 */
    @Schema(description = "Service uptime")
    private String serviceUptime;

    /** CPU 核心数 */
    @Schema(description = "CPU cores")
    private Integer cpuCores;

    /** 主机名称 */
    @Schema(description = "Host name")
    private String hostName;

    /** 操作系统 */
    @Schema(description = "OS version")
    private String osVersion;

    /** 系统总内存(MB) */
    @Schema(description = "System total memory(MB)")
    private Double systemTotalMemoryMb;

    /** 系统可用内存(MB) */
    @Schema(description = "System available memory(MB)")
    private Double systemAvailableMemoryMb;

    /** 最大堆内存(MB) */
    @Schema(description = "Max heap memory(MB)")
    private Double maxHeapMemory;

    /** Heap 使用率(%) */
    @Schema(description = "Heap usage rate(%)")
    private Double heapUsageRate;

    /** CPU 使用率(%) */
    @Schema(description = "CPU usage(%)")
    private Double cpuUsage;

    /** 系统负载 */
    @Schema(description = "System load average")
    private Double loadAverage;

    /** 磁盘使用率(%) */
    @Schema(description = "Disk usage(%)")
    private Double diskUsage;

    /** GC 状态 NORMAL/WARN/ERROR */
    @Schema(description = "GC status")
    private String gcStatus;

    /** Young GC 次数 */
    @Schema(description = "Young GC count")
    private Long youngGcCount;

    /** Full GC 次数 */
    @Schema(description = "Full GC count")
    private Long fullGcCount;

    /** GC 总耗时(ms) */
    @Schema(description = "GC total time(ms)")
    private Long gcTotalTimeMs;


}