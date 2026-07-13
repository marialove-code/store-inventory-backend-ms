package com.inventory.modules.monitor.redismonitor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.monitor.redismonitor.service.RedisTrendService;
import com.inventory.modules.monitor.redismonitor.vo.RedisTrendVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTrendServiceImpl
        implements RedisTrendService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public RedisTrendVo getTrend() {

        String json =
                redisTemplate
                        .opsForValue()
                        .get("monitor:redis:trend");

        if (json == null) {

            return new RedisTrendVo();
        }

        try {

            return objectMapper.readValue(
                    json,
                    RedisTrendVo.class
            );

        } catch (Exception e) {

            return new RedisTrendVo();
        }
    }
}