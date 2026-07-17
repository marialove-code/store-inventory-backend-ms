package com.inventory.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.inventory.common.response.Result;
import com.inventory.order.config.OrderNacosDemoProperties;
import com.inventory.order.config.SentinelResourceNames;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P0 探活 + Nacos Config 演示 + Sentinel 限流演示。
 * <p>
 * 探活 + 限流参考实现；真实业务见 {@code OrderInfoController} / {@code InventoryStockClient}。
 * </p>
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderPingController {

    private final OrderNacosDemoProperties nacosDemoProperties;

    @GetMapping("/ping")
    @SentinelResource(
            value = SentinelResourceNames.ORDER_PING,
            blockHandler = "pingBlockHandler"
    )
    public Result<Map<String, String>> ping() {
        Map<String, String> data = new LinkedHashMap<>(4);
        data.put("service", "order-service");
        data.put("status", "UP");
        data.put("nacosDemoMessage", nacosDemoProperties.getNacosDemoMessage());
        return Result.success(data);
    }

    /**
     * 限流/熔断等 BlockException 时的处理（须 public、与原方法同返回类型，末参为 BlockException）。
     */
    public Result<Map<String, String>> pingBlockHandler(BlockException ex) {
        return Result.fail("触发 Sentinel 限流（资源 orderPing，QPS≤2），请稍后再试："
                + ex.getClass().getSimpleName());
    }
}
