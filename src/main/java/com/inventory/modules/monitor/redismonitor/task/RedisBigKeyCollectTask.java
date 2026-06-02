package com.inventory.modules.monitor.redismonitor.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.monitor.redismonitor.entity.BigKeyItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Redis大Key采集任务
 *
 * 功能：
 * 1. 每10分钟执行一次
 * 2. 使用SCAN遍历Redis
 * 3. 获取Key类型
 * 4. 估算Key大小
 * 5. 取Top50
 * 6. 写入Redis缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBigKeyCollectTask {

    /**
     * Redis缓存Key
     */
    private static final String CACHE_KEY =
            "monitor:redis:bigkey";

    /**
     * 保留Top50
     */
    private static final int TOP_N = 50;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 每10分钟执行一次
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void collect() {

        long startTime = System.currentTimeMillis();

        try {

            RedisConnection connection =
                    Objects.requireNonNull(
                            redisTemplate.getConnectionFactory()
                    ).getConnection();

            List<BigKeyItem> bigKeys =
                    new ArrayList<>();

            Cursor<byte[]> cursor =
                    connection.scan(
                            org.springframework.data.redis.core.ScanOptions
                                    .scanOptions()
                                    .count(200)
                                    .build()
                    );

            while (cursor.hasNext()) {

                byte[] keyBytes =
                        cursor.next();

                String key =
                        new String(
                                keyBytes,
                                StandardCharsets.UTF_8
                        );

                BigKeyItem item =
                        buildBigKeyItem(
                                connection,
                                keyBytes,
                                key
                        );

                if (item != null) {

                    bigKeys.add(item);
                }
            }

            cursor.close();

            /*
             * 按大小降序
             */
            bigKeys.sort(
                    Comparator.comparingLong(
                                    BigKeyItem::getSize
                            )
                            .reversed()
            );

            /*
             * 保留Top50
             */
            if (bigKeys.size() > TOP_N) {

                bigKeys =
                        bigKeys.subList(
                                0,
                                TOP_N
                        );
            }

            redisTemplate
                    .opsForValue()
                    .set(
                            CACHE_KEY,
                            objectMapper.writeValueAsString(
                                    bigKeys
                            )
                    );

            log.info(
                    "Redis大Key采集完成，共{}个，耗时:{}ms",
                    bigKeys.size(),
                    System.currentTimeMillis() - startTime
            );

        } catch (Exception e) {

            log.error(
                    "Redis大Key采集失败",
                    e
            );
        }
    }

    /**
     * 构建BigKey对象
     */
    private BigKeyItem buildBigKeyItem(
            RedisConnection connection,
            byte[] keyBytes,
            String key
    ) {

        try {

            String type =
                    connection.type(keyBytes)
                            .code();

            long size =
                    estimateSize(
                            connection,
                            keyBytes,
                            type
                    );

            Long ttl =
                    connection.ttl(keyBytes);

            BigKeyItem item =
                    new BigKeyItem();

            item.setKey(key);
            item.setType(type);
            item.setSize(size);
            item.setTtl(
                    ttl == null
                            ? -1
                            : ttl
            );

            return item;

        } catch (Exception e) {

            return null;
        }
    }

    /**
     * 估算Key大小
     *
     * 注意：
     * 这里不是字节数
     * 而是数据规模
     *
     * 面试时可以说：
     * 为避免MEMORY USAGE兼容问题
     * 采用数据结构长度估算
     */
    private long estimateSize(
            RedisConnection connection,
            byte[] keyBytes,
            String type
    ) {

        try {

            switch (type) {

                case "string":

                    Long strLen =
                            connection.stringCommands()
                                    .strLen(keyBytes);

                    return strLen == null
                            ? 0
                            : strLen;

                case "hash":

                    Long hLen =
                            connection.hashCommands()
                                    .hLen(keyBytes);

                    return hLen == null
                            ? 0
                            : hLen;

                case "list":

                    Long lLen =
                            connection.listCommands()
                                    .lLen(keyBytes);

                    return lLen == null
                            ? 0
                            : lLen;

                case "set":

                    Long sCard =
                            connection.setCommands()
                                    .sCard(keyBytes);

                    return sCard == null
                            ? 0
                            : sCard;

                case "zset":

                    Long zCard =
                            connection.zSetCommands()
                                    .zCard(keyBytes);

                    return zCard == null
                            ? 0
                            : zCard;

                default:

                    return 0;
            }

        } catch (Exception e) {

            return 0;
        }
    }

    /**
     * 手动触发大Key采集，用于测试接口数据
     */
    public void collectOnce() {
        collect(); // 调用原来的 @Scheduled 方法
    }
}