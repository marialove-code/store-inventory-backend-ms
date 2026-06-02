package com.inventory.modules.monitor.redismonitor.service.impl;

import com.inventory.modules.monitor.redismonitor.service.RedisMonitorService;
import com.inventory.modules.monitor.redismonitor.vo.RedisMonitorVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class RedisMonitorServiceImpl
        implements RedisMonitorService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public RedisMonitorVo getInfo() {
        RedisMonitorVo vo = new RedisMonitorVo();

        RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();

        Properties info = connection.info();
        Properties memoryInfo = connection.info("memory");
        Properties replicationInfo = connection.info("replication");
        Properties persistenceInfo = connection.info("persistence");

        // Redis版本
        vo.setRedisVersion(info.getProperty("redis_version"));

        // 状态
        vo.setStatus("running");

        // 运行时长
        long uptimeSeconds = Long.parseLong(info.getProperty("uptime_in_seconds", "0"));
        vo.setUptime(formatUptime(uptimeSeconds));

        // 当前连接数
        vo.setConnectionCount(Integer.parseInt(info.getProperty("connected_clients", "0")));

        // 最大连接数
        vo.setMaxClients(Integer.parseInt(info.getProperty("maxclients", "0")));

        // 已使用内存
        long usedMemory = Long.parseLong(info.getProperty("used_memory", "0"));
        double usedMemoryMb = usedMemory / 1024D / 1024D;
        vo.setMemoryUsage(usedMemoryMb);

        // 最大内存
        long maxMemory = Long.parseLong(memoryInfo.getProperty("maxmemory", "0"));
        double maxMemoryMb = maxMemory <= 0 ? 0 : maxMemory / 1024D / 1024D;
        vo.setMaxMemory(maxMemoryMb);

        // 内存使用率 保留2位小数
        if (maxMemoryMb > 0) {
            double rate = usedMemoryMb * 100 / maxMemoryMb;
            vo.setMemoryUsageRate(Math.round(rate * 100D) / 100D);
        } else {
            vo.setMemoryUsageRate(0D);
        }

        // Redis角色
        vo.setRole(replicationInfo.getProperty("role", "master"));

        // 数据库数量
        int dbCount = 0;
        for (Object key : info.keySet()) {
            if (String.valueOf(key).startsWith("db")) dbCount++;
        }
        vo.setDatabaseCount(dbCount);

        // 命中率
        long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
        long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
        vo.setHitRate(hits + misses > 0 ? hits * 100D / (hits + misses) : 100D);

        // QPS
        vo.setCommandQps(Double.parseDouble(info.getProperty("instantaneous_ops_per_sec", "0")));

        // 拒绝连接次数
        vo.setRejectedConnections(Long.parseLong(info.getProperty("rejected_connections", "0")));

        // Key统计
        long keyTotal = 0, expiredKeyTotal = 0;
        for (Object key : info.keySet()) {
            String db = String.valueOf(key);
            if (!db.startsWith("db")) continue;
            String[] arr = info.getProperty(db).split(",");
            for (String item : arr) {
                if (item.startsWith("keys=")) keyTotal += Long.parseLong(item.replace("keys=", ""));
                if (item.startsWith("expires=")) expiredKeyTotal += Long.parseLong(item.replace("expires=", ""));
            }
        }
        vo.setKeyTotal(keyTotal);
        vo.setExpiredKeyCount(expiredKeyTotal);

        // Key过期速率
        vo.setExpiredRate(uptimeSeconds > 0 ? expiredKeyTotal * 60D / uptimeSeconds : 0D);


       // 内存碎片率 保留两位小数
        double fragmentationRatio = Double.parseDouble(info.getProperty("mem_fragmentation_ratio", "0"));
        vo.setMemFragmentationRatio(Math.round(fragmentationRatio * 100D) / 100D);

        // AOF开关
        vo.setAofEnabled("1".equals(persistenceInfo.getProperty("aof_enabled", "0")));

        // RDB与最近持久化时间
        String lastSaveTime = persistenceInfo.getProperty("rdb_last_save_time");
        if (lastSaveTime != null) {
            vo.setRdbEnabled(true);
            vo.setLastSaveTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(lastSaveTime)), ZoneId.systemDefault()).toString());
        } else {
            vo.setRdbEnabled(false);
        }

        // 累计处理命令总数
        vo.setTotalCommandsProcessed(Long.parseLong(info.getProperty("total_commands_processed", "0")));
        // 累计接入连接总数
        vo.setTotalConnectionsReceived(Long.parseLong(info.getProperty("total_connections_received", "0")));

        // 运行模式：单机/主从
        String role = replicationInfo.getProperty("role", "master");
        int connectedSlaves = Integer.parseInt(replicationInfo.getProperty("connected_slaves", "0"));
        vo.setRunMode(connectedSlaves > 0 || "slave".equals(role) ? "主从模式" : "单机模式");

        return vo;
    }

    /**
     * 秒转为可读运行时长
     */
    private String formatUptime(long seconds) {
        long day = seconds / 86400;
        long hour = (seconds % 86400) / 3600;
        long minute = (seconds % 3600) / 60;
        return day + "天" + hour + "小时" + minute + "分钟";
    }
}