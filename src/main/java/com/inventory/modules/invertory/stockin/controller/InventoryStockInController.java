package com.inventory.modules.invertory.stockin.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.service.InventoryInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 入库管理控制层
 * 功能：接收前端请求 → 转发给Service → 直接返回结果
 * 特点：无任何业务逻辑，只做路由转发
 */
@RestController
@RequestMapping("/inventory/stockin")
@RequiredArgsConstructor
public class InventoryStockInController {

    /**
     * 注入入库管理服务
     */
    private final InventoryInService stockInService;

    /**
     * 入库列表分页查询
     * 筛选条件：入库单号、商品名称、入库时间范围
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String receiptNo,
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return stockInService.pageStockIn(receiptNo, goodsName, startTime, endTime, pageNum, pageSize);
    }

    /**
     * 入库详情查询
     * @param id 入库单ID
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return stockInService.getStockInDetail(id);
    }

    /**
     * 新增入库单
     * @param dto 入库单参数
     */
    @PostMapping
    public Result<?> add(@RequestBody StockInAddDTO dto) {
        return stockInService.addStockIn(dto);
    }
}