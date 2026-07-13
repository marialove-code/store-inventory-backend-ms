package com.inventory.modules.invertory.stockin.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.service.InventoryInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 入库管理控制层。
 * <p>微服务阶段去掉 {@code @RequiresPerm}，本阶段不做鉴权。</p>
 */
@RestController
@RequestMapping("/inventory/stockin")
@RequiredArgsConstructor
public class InventoryStockInController {

    private final InventoryInService stockInService;

    /** 入库列表分页查询 */
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

    /** 入库单详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return stockInService.getStockInDetail(id);
    }

    /** 新增入库单 */
    @PostMapping
    public Result<?> add(@RequestBody StockInAddDTO dto) {
        return stockInService.addStockIn(dto);
    }
}
