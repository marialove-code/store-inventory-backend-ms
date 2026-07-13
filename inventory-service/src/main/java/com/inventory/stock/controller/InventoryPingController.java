package com.inventory.stock.controller;

import com.inventory.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P0 探活接口：确认库存服务进程与 context-path 正常。
 */
@RestController
@RequestMapping("/inventory")
public class InventoryPingController {

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        return Result.success(Map.of(
                "service", "inventory-service",
                "status", "UP"
        ));
    }
}
