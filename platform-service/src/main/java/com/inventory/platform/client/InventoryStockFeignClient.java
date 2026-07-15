package com.inventory.platform.client;

import com.inventory.common.client.dto.StockInitRequest;
import com.inventory.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 库存服务 Feign 客户端（平台侧）。
 * <p>
 * {@code name} 与 inventory-service 的 {@code spring.application.name} 一致；
 * {@code path = /api} 对应对方 context-path。
 * </p>
 */
@FeignClient(name = "inventory-service", path = "/api")
public interface InventoryStockFeignClient {

    @PostMapping("/inventory/internal/init-stock")
    Result<Void> initStock(@RequestBody StockInitRequest request);

    @GetMapping("/inventory/internal/usable")
    Result<Map<String, Object>> usable(@RequestParam("goodsId") Long goodsId);
}
