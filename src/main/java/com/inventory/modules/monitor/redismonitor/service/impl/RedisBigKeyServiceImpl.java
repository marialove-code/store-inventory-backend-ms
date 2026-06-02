package com.inventory.modules.monitor.redismonitor.service.impl;
import com.inventory.modules.monitor.redismonitor.vo.PageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.monitor.redismonitor.entity.BigKeyItem;
import com.inventory.modules.monitor.redismonitor.service.RedisBigKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Redis 大 Key 服务实现
 *
 * 数据来自缓存 monitor:redis:bigkey
 */
@Service
@RequiredArgsConstructor
public class RedisBigKeyServiceImpl implements RedisBigKeyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY = "monitor:redis:bigkey";

    @Override
    public PageResult<BigKeyItem> getBigKeyPage(int page, int size) {

        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY);

            if (json == null || json.isEmpty()) {
                return PageResult.of(0, Collections.emptyList());
            }

            List<BigKeyItem> allKeys = objectMapper.readValue(
                    json, new TypeReference<List<BigKeyItem>>() {}
            );

            int total = allKeys.size();
            int start = Math.max((page - 1) * size, 0);
            int end = Math.min(start + size, total);

            List<BigKeyItem> records = allKeys.subList(start, end);

            return PageResult.of(total, records);

        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.of(0, Collections.emptyList());
        }
    }
}