package com.inventory.modules.shop.records.controller;

import com.inventory.common.page.PageResult;
import com.inventory.common.response.Result;
import com.inventory.modules.shop.records.dto.ShopSaleCreateDto;
import com.inventory.modules.shop.records.entity.ShopSaleRecordListParam;
import com.inventory.modules.shop.records.service.ShopSaleRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 销售开单 & 流水
 */
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopSaleController {

    private final ShopSaleRecordService shopSaleRecordService;

    /**
     * 销售流水列表
     */
    @GetMapping("/record/list")
    public Result<?>recordList(ShopSaleRecordListParam param) {
        return shopSaleRecordService.getSaleRecordPage(param);
    }

    /**
     * 开单记账
     */
    @PostMapping("/sale")
    public Result<?>createSale(@Valid @RequestBody ShopSaleCreateDto dto) {
        return shopSaleRecordService.createSaleOrder(dto);
    }

    /**
     * 今日/本月营收统计
     */
    @GetMapping("/sale/stats")
    public Result<?> stats() {
        return shopSaleRecordService.getSaleStatistics();
    }

    /**
     * 逻辑删除销售流水
     */
    @DeleteMapping("/record/{id}")
    public Result<?> deleteRecord(@PathVariable Long id) {
        return shopSaleRecordService.deleteSaleRecord(id);
    }
}