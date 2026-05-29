package com.inventory.modules.invertory.stock.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.dto.StockWarnDTO;
import com.inventory.modules.invertory.stock.service.InventoryStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存列表控制层
 * 功能：接收前端请求 → 转发给Service → 直接返回结果
 * 特点：无任何业务逻辑，只做路由转发
 */
@RestController
@RequestMapping("/inventory/stock")
@RequiredArgsConstructor
public class StockController {

    /**
     * 注入库存服务
     */
    private final InventoryStockService stockService;

    /**
     * 库存列表分页查询
     * 筛选条件：商品名称、商品分类名称、库存状态
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
     * 编辑库存预警阈值
     * @param id 库存数据ID
     * @param dto 预警阈值参数
     */
    @PutMapping("/list/{id}/stockWarn")
    public Result<?> editStockWarn(@PathVariable String id, @RequestBody StockWarnDTO dto) {
        return stockService.updateStockWarn(id, dto);
    }
}