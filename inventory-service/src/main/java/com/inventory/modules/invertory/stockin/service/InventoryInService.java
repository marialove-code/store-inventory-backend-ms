package com.inventory.modules.invertory.stockin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.entity.InventoryIn;

/**
 * 入库单业务接口。
 */
public interface InventoryInService extends IService<InventoryIn> {

    /** 入库单分页查询 */
    Result<?> pageStockIn(String receiptNo, String goodsName, String startTime, String endTime,
                          Long pageNum, Long pageSize);

    /** 入库单详情 */
    Result<?> getStockInDetail(Long id);

    /** 新增入库单（写单 + 增加库存） */
    Result<?> addStockIn(StockInAddDTO dto);
}
