package com.inventory.modules.invertory.stockflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockflow.dto.InventoryFlowQueryDTO;
import com.inventory.modules.invertory.stockflow.entity.InventoryFlow;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 库存流水业务接口。
 */
public interface InventoryFlowService extends IService<InventoryFlow> {

    /** 流水分页列表 */
    Result<?> pageFlowList(InventoryFlowQueryDTO queryDTO);

    /** 流水详情 */
    Result<?> getFlowDetail(String id);

    /** 导出流水（当前为占位实现，与单体一致） */
    void exportFlowList(InventoryFlowQueryDTO queryDTO, HttpServletResponse response) throws IOException;
}
