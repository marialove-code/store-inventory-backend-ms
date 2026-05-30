package com.inventory.modules.invertory.stockflow.service;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockflow.dto.InventoryFlowQueryDTO;
import com.inventory.modules.invertory.stockflow.entity.InventoryFlow;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
* @author 95349
* @description 针对表【inventory_flow(库存流水表)】的数据库操作Service
* @createDate 2026-05-29 19:11:19
*/
public interface InventoryFlowService extends IService<InventoryFlow> {

    /**
     * 库存流水分页列表查询
     */
    Result<?> pageFlowList(InventoryFlowQueryDTO queryDTO);

    /**
     * 获取库存流水详情
     */
    Result<?> getFlowDetail(String id);

    /**
     * 导出库存流水Excel
     */
    void exportFlowList(InventoryFlowQueryDTO queryDTO, HttpServletResponse response) throws IOException;
}
