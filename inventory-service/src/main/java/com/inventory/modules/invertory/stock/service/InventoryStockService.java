package com.inventory.modules.invertory.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.dto.StockWarnDTO;
import com.inventory.modules.invertory.stock.entity.InventoryStock;

/**
 * 库存实时信息 Service（列表查询、预警修改）。
 * <p>对应表 {@code inventory_stock}；与核心 {@link StockService} 职责分离。</p>
 */
public interface InventoryStockService extends IService<InventoryStock> {

    /**
     * 库存分页列表查询。
     * 筛选条件：商品名称、商品分类名称、库存状态。
     */
    Result<?> pageStock(String goodsName, String categoryName, Integer stockStatus,
                        Long pageNum, Long pageSize);

    /**
     * 修改库存预警阈值（及可选的可用库存）。
     */
    Result<?> updateStockWarn(String id, StockWarnDTO dto);
}
