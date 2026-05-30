package com.inventory.modules.invertory.stockwarn.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockwarn.dto.StockWarnDTO;
import com.inventory.modules.invertory.stockwarn.service.InventoryWarnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存预警控制层
 * 功能：处理库存预警相关接口（列表、详情、修改预警阈值）
 * 权限：inventory:warn:list / detail / edit
 */
@RestController
@RequestMapping("/inventory/warn")
@RequiredArgsConstructor
public class InventoryWarnController {

    /**
     * 注入库存预警服务
     */
    private final InventoryWarnService inventoryWarnService;

    /**
     * 库存预警列表分页查询
     * 筛选条件：商品名称模糊查询
     * 权限：inventory:warn:list
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String goodsName,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return inventoryWarnService.pageWarnList(goodsName, pageNum, pageSize);
    }

    /**
     * 查询库存预警单条详情
     * 根据ID获取预警商品详情
     * 权限：inventory:warn:detail
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        return inventoryWarnService.getWarnDetail(id);
    }

    /**
     * 修改商品库存预警阈值
     * @param id 库存记录ID
     * @param dto 预警阈值参数
     * 权限：inventory:warn:edit
     */
    @PutMapping("/{id}/stockWarn")
    public Result<?> editStockWarn(
            @PathVariable String id,
            @RequestBody StockWarnDTO dto
    ) {
        return inventoryWarnService.updateStockWarn(id, dto);
    }
}