package com.inventory.modules.invertory.stockout.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.service.InventoryOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 出库管理控制层
 * 功能：接收前端请求 → 转发给Service → 直接返回结果
 * 特点：无任何业务逻辑，只做路由转发
 */
@RestController
@RequestMapping("/inventory/stockout")
@RequiredArgsConstructor
public class StockOutController {

    /**
     * 注入出库管理服务
     */
    private final InventoryOutService stockOutService;

    /**
     * 出库列表分页查询
     * 筛选条件：出库单号、商品名称、出库时间范围
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String outboundNo,
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return stockOutService.pageStockOut(outboundNo, goodsName, startTime, endTime, pageNum, pageSize);
    }

    /**
     * 出库详情查询
     * @param id 出库单ID
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return stockOutService.getStockOutDetail(id);
    }

    /**
     * 新增出库单
     * @param dto 出库单参数
     */
    @PostMapping
    public Result<?> add(@RequestBody StockOutAddDTO dto) {
        return stockOutService.addStockOut(dto);
    }
}