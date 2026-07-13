package com.inventory.modules.invertory.stock.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.dto.StockWarnDTO;
import com.inventory.modules.invertory.stock.service.InventoryStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存列表控制层（面向前端管理页）。
 * <p>
 * 功能：接收前端请求 → 转发给 Service → 直接返回结果。
 * 适配说明：微服务 P1 去掉 {@code @RequiresPerm}，本阶段不做鉴权。
 * </p>
 */
@RestController
@RequestMapping("/inventory/stock")
@RequiredArgsConstructor
public class StockController {

    private final InventoryStockService stockService;

    /**
     * 库存列表分页查询。
     * 筛选条件：商品名称、商品分类名称、库存状态。
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Integer stockStatus,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return stockService.pageStock(goodsName, categoryName, stockStatus, pageNum, pageSize);
    }

    /**
     * 编辑库存预警阈值。
     *
     * @param id  库存数据 ID
     * @param dto 预警阈值参数
     */
    @PutMapping("/list/{id}/stockWarn")
    public Result<?> editStockWarn(@PathVariable String id, @RequestBody StockWarnDTO dto) {
        return stockService.updateStockWarn(id, dto);
    }
}
