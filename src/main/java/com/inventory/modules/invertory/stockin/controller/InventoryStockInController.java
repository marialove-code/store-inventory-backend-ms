package com.inventory.modules.invertory.stockin.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.service.InventoryInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 入库管理控制层
 * 功能：接收前端请求 → 转发给Service → 直接返回结果
 * 特点：无任何业务逻辑，只做路由转发+参数接收
 * 接口规范：
 * - GET /inventory/stockin/list ：分页查询入库单
 * - GET /inventory/stockin/{id} ：查询入库单详情
 * - POST /inventory/stockin     ：新增入库单
 */
@RestController
@RequestMapping("/inventory/stockin")
@RequiredArgsConstructor
public class InventoryStockInController {

    /**
     * 注入入库管理服务（构造器注入：避免NPE，符合Spring最佳实践）
     */
    private final InventoryInService stockInService;

    /**
     * 入库列表分页查询
     * 筛选条件：入库单号、商品名称、入库时间范围
     * 请求方式：GET
     * 请求参数：均为非必填，前端可根据筛选条件传递
     */
    @GetMapping("/list")
    @RequiresPerm("inventory:stockin:list")
    public Result<?> list(
            @RequestParam(required = false) String receiptNo,    // 入库单号（精准匹配）
            @RequestParam(required = false) String goodsName,   // 商品名称（模糊匹配）
            @RequestParam(required = false) String startTime,   // 开始时间（yyyy-MM-dd HH:mm:ss）
            @RequestParam(required = false) String endTime,     // 结束时间（yyyy-MM-dd HH:mm:ss）
            @RequestParam(defaultValue = "1") Long pageNum,     // 页码（默认1）
            @RequestParam(defaultValue = "10") Long pageSize    // 每页条数（默认10）
    ) {
        return stockInService.pageStockIn(receiptNo, goodsName, startTime, endTime, pageNum, pageSize);
    }


    /**
     * 新增入库单
     * 请求方式：POST
     * 请求体：StockInAddDTO（已做参数校验）
     */
    @PostMapping
    @RequiresPerm("inventory:stockin:add")
    public Result<?> add(@RequestBody StockInAddDTO dto) {
        return stockInService.addStockIn(dto);
    }
}