package com.inventory.platform.controller;

import com.inventory.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台服务探活接口。
 * <p>
 * 供网关、运维或本地联调确认 platform-service（8081）已启动。
 * </p>
 */
@RestController
@RequestMapping("/platform")
public class PlatformPingController {

    /**
     * 探活：GET /platform/ping
     */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>(4);
        data.put("service", "platform-service");
        data.put("status", "UP");
        return Result.success(data);
    }
}
