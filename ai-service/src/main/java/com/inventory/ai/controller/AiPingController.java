package com.inventory.ai.controller;

import com.inventory.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 服务探活。
 */
@RestController
@RequestMapping("/ai")
public class AiPingController {

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        return Result.success(Map.of(
                "service", "ai-service",
                "status", "UP"
        ));
    }
}
