package com.inventory.modules.invertory.stockout.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.service.InventoryOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 出库管理控制层
 * 功能：接收前端请求 → 转发给Service → 直接返回结果
 * 特点：无任何业务逻辑，只做路由转发+参数接收
 * 接口规范：
 * - GET /inventory/stockout/list ：分页查询出库单
 * - GET /inventory/stockout/{id} ：查询出库单详情
 * - POST /inventory/stockout     ：新增出库单
 */
@RestController
@RequestMapping("/inventory/stockout")
@RequiredArgsConstructor
public class InventoryOutController {

    /**
     * 注入出库管理服务（构造器注入：避免NPE，符合Spring最佳实践）
     */
    private final InventoryOutService stockOutService;

    /**
     * 出库列表分页查询
     * 筛选条件：出库单号、商品名称、出库时间范围
     * 请求方式：GET
     * 请求参数：均为非必填，前端可根据筛选条件传递
     */
    @GetMapping("/list")
    @RequiresPerm("inventory:stockout:list")
    public Result<?> list(
            @RequestParam(required = false) String outboundNo,   // 出库单号（精准匹配）
            @RequestParam(required = false) String goodsName,    // 商品名称（模糊匹配）
            @RequestParam(required = false) String startTime,    // 开始时间（yyyy-MM-dd HH:mm:ss）
            @RequestParam(required = false) String endTime,      // 结束时间（yyyy-MM-dd HH:mm:ss）
            @RequestParam(defaultValue = "1") Long pageNum,      // 页码（默认1）
            @RequestParam(defaultValue = "10") Long pageSize     // 每页条数（默认10）
    ) {
        return stockOutService.pageStockOut(outboundNo, goodsName, startTime, endTime, pageNum, pageSize);
    }

    /**
     * 新增出库单
     * 请求方式：POST
     * 请求体：StockOutAddDTO（已做参数校验）
     */
    @PostMapping
    @RequiresPerm("inventory:stockout:add")
    public Result<?> add(@RequestBody StockOutAddDTO dto) {
        return stockOutService.addStockOut(dto);
    }
}