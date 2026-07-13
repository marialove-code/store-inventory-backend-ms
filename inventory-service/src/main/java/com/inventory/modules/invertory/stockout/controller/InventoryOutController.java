package com.inventory.modules.invertory.stockout.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.service.InventoryOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 出库管理控制层。
 * <p>微服务阶段去掉 {@code @RequiresPerm}，本阶段不做鉴权。</p>
 */
@RestController
@RequestMapping("/inventory/stockout")
@RequiredArgsConstructor
public class InventoryOutController {

    private final InventoryOutService stockOutService;

    /** 出库列表分页查询 */
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

    /** 出库单详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return stockOutService.getStockOutDetail(id);
    }

    /** 新增出库单 */
    @PostMapping
    public Result<?> add(@RequestBody StockOutAddDTO dto) {
        return stockOutService.addStockOut(dto);
    }
}
