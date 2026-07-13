package com.inventory.modules.invertory.stockflow.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockflow.dto.InventoryFlowQueryDTO;
import com.inventory.modules.invertory.stockflow.service.InventoryFlowService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 库存流水控制层。
 * <p>微服务阶段去掉 {@code @RequiresPerm}，本阶段不做鉴权。</p>
 */
@RestController
@RequestMapping("/inventory/flow")
@RequiredArgsConstructor
public class InventoryFlowController {

    private final InventoryFlowService inventoryFlowService;

    /** 库存流水分页列表 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String operateType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        InventoryFlowQueryDTO queryDTO = new InventoryFlowQueryDTO();
        queryDTO.setGoodsName(goodsName);
        queryDTO.setOperateType(operateType);
        queryDTO.setStartTime(startTime);
        queryDTO.setEndTime(endTime);
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);
        return inventoryFlowService.pageFlowList(queryDTO);
    }

    /** 流水详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        return inventoryFlowService.getFlowDetail(id);
    }

    /** 导出库存流水（占位） */
    @PostMapping("/export")
    public void export(@RequestBody InventoryFlowQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        inventoryFlowService.exportFlowList(queryDTO, response);
    }
}
