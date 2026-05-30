package com.inventory.modules.invertory.stockflow.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockflow.dto.InventoryFlowQueryDTO;
import com.inventory.modules.invertory.stockflow.service.InventoryFlowService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

/**
 * 库存流水控制层
 * 功能：处理库存流水列表、详情、导出接口
 * 权限：inventory:flow:list / detail / export
 */
@RestController
@RequestMapping("/inventory/flow")
@RequiredArgsConstructor
public class InventoryFlowController {

    /**
     * 注入库存流水服务
     */
    private final InventoryFlowService inventoryFlowService;

    /**
     * 库存流水列表分页查询
     * 筛选条件：商品名称、操作类型、时间范围
     * 权限：inventory:flow:list
     */
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

    /**
     * 库存流水详情查询
     * @param id 流水记录ID
     * 权限：inventory:flow:detail
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        return inventoryFlowService.getFlowDetail(id);
    }

    /**
     * 导出库存流水Excel
     * 权限：inventory:flow:export
     */
    @PostMapping("/export")
    public void export(@RequestBody InventoryFlowQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        inventoryFlowService.exportFlowList(queryDTO, response);
    }
}