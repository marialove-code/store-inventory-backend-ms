package com.inventory.modules.monitor.redismonitor.vo;

import com.inventory.modules.monitor.redismonitor.entity.BigKeyItem;
import com.inventory.modules.monitor.redismonitor.entity.TrendItem;
import lombok.Data;

import java.util.List;

/**
 * Redis监控信息
 *
 * 首页展示：
 * Redis版本
 * 运行状态
 * 运行时长
 * 连接数
 *
 * 高级指标：
 * 最大内存
 * 内存使用率
 * Redis角色
 * 数据库数量
 * 拒绝连接次数
 * Key过期速率
 *
 * 图表：
 * 内存趋势
 * 命中率趋势
 * QPS趋势
 *
 * 表格：
 * Key统计
 * 大Key列表
 */
@Data
public class RedisMonitorVo {

    /**
     * Redis版本
     * 示例：8.0.5
     */
    private String redisVersion;

    /**
     * Redis状态
     *
     * running：正常
     * warning：警告
     * error：异常
     */
    private String status;

    /**
     * Redis运行时长
     * 示例：5天12小时36分钟
     */
    private String uptime;

    /**
     * Redis角色
     *
     * master：主节点
     * slave：从节点
     */
    private String role;

    /**
     * Redis运行模式
     *
     * Standalone：单机模式
     * Cluster：集群模式
     */
    private String runMode;

    /**
     * 当前连接数
     */
    private Integer connectionCount;

    /**
     * 最大连接数
     */
    private Integer maxClients;

    /**
     * 拒绝连接次数
     * 来源：rejected_connections
     */
    private Long rejectedConnections;

    /**
     * 已使用内存(MB)
     */
    private Double memoryUsage;

    /**
     * 最大可用内存(MB)
     */
    private Double maxMemory;

    /**
     * 内存使用率(%)
     */
    private Double memoryUsageRate;

    /**
     * Redis内存碎片率
     * 来源：mem_fragmentation_ratio
     *
     * 正常范围：
     * 1.0 ~ 1.5
     */
    private Double memFragmentationRatio;

    /**
     * 数据库数量
     * 示例：16
     */
    private Integer databaseCount;

    /**
     * Key总数
     */
    private Long keyTotal;

    /**
     * 已过期Key数量
     */
    private Long expiredKeyCount;

    /**
     * Key过期速率
     * 单位：个/分钟
     */
    private Double expiredRate;

    /**
     * 缓存命中率(%)
     */
    private Double hitRate;

    /**
     * 当前QPS
     * 每秒执行命令数
     */
    private Double commandQps;


    /**
     * Redis累计处理命令数
     * 来源：total_commands_processed
     */
    private Long totalCommandsProcessed;

    /**
     * Redis累计连接次数
     * 来源：total_connections_received
     */
    private Long totalConnectionsReceived;

    /**
     * 是否开启AOF持久化
     */
    private Boolean aofEnabled;

    /**
     * 是否开启RDB持久化
     */
    private Boolean rdbEnabled;

    /**
     * 最近一次RDB持久化时间
     */
    private String lastSaveTime;

    /**
     * 内存使用趋势
     */
    private List<TrendItem> memoryUsageTrend;

    /**
     * 缓存命中率趋势
     */
    private List<TrendItem> hitRateTrend;

    /**
     * QPS趋势
     */
    private List<TrendItem> commandQpsTrend;

    /**
     * 大Key列表
     */
    private List<BigKeyItem> bigKeyList;
}
