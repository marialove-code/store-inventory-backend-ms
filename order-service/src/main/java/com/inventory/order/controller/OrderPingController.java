package com.inventory.order.controller;

import com.inventory.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P0 探活接口：确认订单服务进程与 context-path 正常。
 */
@RestController
@RequestMapping("/order")
public class OrderPingController {

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        return Result.success(Map.of(
                "service", "order-service",
                "status", "UP"
        ));
    }
}
