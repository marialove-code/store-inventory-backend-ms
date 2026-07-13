package com.inventory.modules.invertory.stockwarn.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockwarn.dto.StockWarnDTO;
import com.inventory.modules.invertory.stockwarn.service.InventoryWarnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存预警控制层。
 * <p>微服务阶段去掉 {@code @RequiresPerm}，本阶段不做鉴权。</p>
 */
@RestController
@RequestMapping("/inventory/warn")
@RequiredArgsConstructor
public class InventoryWarnController {

    private final InventoryWarnService inventoryWarnService;

    /** 预警列表：仅返回 stock &lt; stock_warn 的记录 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String goodsName,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return inventoryWarnService.pageWarnList(goodsName, pageNum, pageSize);
    }

    /** 预警详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        return inventoryWarnService.getWarnDetail(id);
    }

    /** 修改预警阈值 */
    @PutMapping("/{id}/stockWarn")
    public Result<?> editStockWarn(@PathVariable String id, @RequestBody StockWarnDTO dto) {
        return inventoryWarnService.updateStockWarn(id, dto);
    }
}
