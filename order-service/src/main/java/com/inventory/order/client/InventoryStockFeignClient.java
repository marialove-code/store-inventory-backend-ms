package com.inventory.order.client;

import com.inventory.common.client.dto.LockStockFlowContext;
import com.inventory.common.client.dto.ResetStockRequest;
import com.inventory.common.client.dto.WriteFlowRequest;
import com.inventory.common.response.Result;
import com.inventory.order.client.dto.StockCommandRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 库存服务 Feign 客户端（按服务名发现，不再写死 localhost:8082）。
 * <p>
 * {@code name} 必须与 inventory-service 的 {@code spring.application.name} 一致；
 * {@code path = /api} 对应对方 {@code server.servlet.context-path}（Nacos 只注册 host:port，不含 context-path）。
 * </p>
 */
@FeignClient(name = "inventory-service", path = "/api")
public interface InventoryStockFeignClient {

    @PostMapping("/inventory/internal/lock")
    Result<Void> lock(@RequestBody StockCommandRequest req);

    @PostMapping("/inventory/internal/unlock")
    Result<Void> unlock(@RequestBody StockCommandRequest req);

    @PostMapping("/inventory/internal/decrease-flow")
    Result<Void> decreaseFlow(@RequestBody StockCommandRequest req);

    @PostMapping("/inventory/internal/increase")
    Result<Void> increase(@RequestBody StockCommandRequest req);

    @PostMapping("/inventory/internal/lock-update-only")
    Result<LockStockFlowContext> lockUpdateOnly(@RequestBody StockCommandRequest req);

    @PostMapping("/inventory/internal/write-flow")
    Result<Void> writeFlow(@RequestBody WriteFlowRequest req);

    @PostMapping("/inventory/internal/dev/reset-stock")
    Result<Void> resetStock(@RequestBody ResetStockRequest req);

    @GetMapping("/inventory/internal/usable")
    Result<Map<String, Object>> usable(@RequestParam("goodsId") Long goodsId);
}
