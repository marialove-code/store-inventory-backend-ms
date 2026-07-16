package com.inventory.order.controller;

import com.inventory.common.response.Result;
import com.inventory.order.config.OrderNacosDemoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P0 探活接口：确认订单服务进程与 context-path 正常；
 * 并回显 Nacos Config 演示字段（治理层第 8 步）。
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderPingController {

    private final OrderNacosDemoProperties nacosDemoProperties;

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        Map<String, String> data = new LinkedHashMap<>(4);
        data.put("service", "order-service");
        data.put("status", "UP");
        // 若 Nacos 已配置 order-service.yaml 且覆盖了 app.nacos-demo-message，这里会显示远程值
        data.put("nacosDemoMessage", nacosDemoProperties.getNacosDemoMessage());
        return Result.success(data);
    }
}
