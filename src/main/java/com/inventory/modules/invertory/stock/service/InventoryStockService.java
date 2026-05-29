package com.inventory.modules.invertory.stock.service;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.dto.StockWarnDTO;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 95349
* @description 针对表【inventory_stock】的数据库操作Service
* @createDate 2026-05-29 19:04:18
*/
public interface InventoryStockService extends IService<InventoryStock> {

    /**
     * 库存分页列表查询
     * 筛选条件：商品名称、商品分类名称、库存状态
     */
    Result<?> pageStock(String goodsName, String categoryName, Integer stockStatus,
                        Long pageNum, Long pageSize);

    /**
     * 修改库存预警阈值
     */
    Result<?> updateStockWarn(String id, StockWarnDTO dto);
}
