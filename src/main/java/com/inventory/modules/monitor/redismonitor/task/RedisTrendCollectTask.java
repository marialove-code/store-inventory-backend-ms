package com.inventory.modules.monitor.redismonitor.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.monitor.redismonitor.entity.TrendItem;
import com.inventory.modules.monitor.redismonitor.vo.RedisTrendVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Redis 监控指标定时采集任务
 * 每分钟执行一次，采集 Redis 内存、命中率、QPS 并保存到 Redis 中，用于前端图表展示
 *
 * @author 系统自动生成
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTrendCollectTask {

    /**
     * Redis 操作模板
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 定时任务：每分钟执行一次（cron表达式：0秒 每分钟 每时 每日 每月 每周）
     * 用于采集 Redis 运行时的关键指标：内存使用率、缓存命中率、命令执行QPS
     */
    @Scheduled(cron = "0 * * * * ?")
    public void collect() {
        try {
            // 1. 获取 Redis 连接
            RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();

            // 2. 获取 Redis 服务运行信息（info命令）
            Properties info = connection.info();

            // ===================== 采集：已使用内存 =====================
            // 从Redis info中获取 used_memory（字节）
            long usedMemory = Long.parseLong(info.getProperty("used_memory", "0"));
            // 转换为 MB
            double memoryMb = usedMemory / 1024D / 1024D;

            // ===================== 采集：缓存命中率 =====================
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));    // 命中次数
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));// 未命中次数
            // 计算命中率百分比
            double hitRate = hits + misses == 0 ? 100 : hits * 100D / (hits + misses);

            // ===================== 采集：每秒执行命令数（QPS） =====================
            double qps = Double.parseDouble(info.getProperty("instantaneous_ops_per_sec", "0"));

            // 3. 从 Redis 中加载历史趋势数据
            RedisTrendVo trendVo = loadTrend();

            // 4. 将本次采集的数据追加到趋势列表中
            append(trendVo.getMemoryUsageTrend(), memoryMb);      // 内存趋势
            append(trendVo.getHitRateTrend(), hitRate);            // 命中率趋势
            append(trendVo.getCommandQpsTrend(), qps);             // QPS 趋势

            // 5. 将更新后的趋势数据保存回 Redis（key=monitor:redis:trend）
            redisTemplate.opsForValue().set(
                    "monitor:redis:trend",
                    objectMapper.writeValueAsString(trendVo)
            );

        } catch (Exception e) {
            // 异常捕获，避免定时任务崩溃
            log.error("Redis趋势采集失败", e);
        }
    }

    /**
     * 从 Redis 加载历史趋势数据
     * 如果不存在则初始化一个空的 RedisTrendVo 对象
     *
     * @return 封装好的 Redis 趋势 VO 对象
     */
    private RedisTrendVo loadTrend() {
        try {
            // 从 Redis 获取历史趋势JSON字符串
            String json = redisTemplate.opsForValue().get("monitor:redis:trend");

            // 第一次运行，Redis 中无数据 → 初始化
            if (json == null) {
                RedisTrendVo vo = new RedisTrendVo();
                vo.setMemoryUsageTrend(new ArrayList<>());
                vo.setHitRateTrend(new ArrayList<>());
                vo.setCommandQpsTrend(new ArrayList<>());
                return vo;
            }

            // 反序列化为对象
            return objectMapper.readValue(json, RedisTrendVo.class);

        } catch (Exception e) {
            // 异常时返回空对象，防止程序报错
            return new RedisTrendVo();
        }
    }

    /**
     * 追加一条指标数据到趋势列表
     * 自动记录当前时间（HH:mm），并限制列表最多保存 30 条（30分钟数据）
     *
     * @param list  趋势列表
     * @param value 当前指标值
     */
    private void append(List<TrendItem> list, double value) {
        if (list == null) {
            return;
        }

        // 构建趋势项
        TrendItem item = new TrendItem();
        // 设置时间：当前小时:分钟
        item.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        // 设置指标值
        item.setValue(value);

        // 添加到列表
        list.add(item);

        // 限制只保留最近 30 分钟的数据
        if (list.size() > 30) {
            list.remove(0);
        }
    }

    /**
     * 手动触发趋势图采集，用于测试接口数据
     */
    public void collectOnce() {
        collect(); // 调用原来的 @Scheduled 方法
    }
}