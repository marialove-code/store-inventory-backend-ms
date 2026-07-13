package com.inventory.modules.invertory.stockout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;

/**
 * 出库单业务接口。
 */
public interface InventoryOutService extends IService<InventoryOut> {

    /** 出库单分页查询 */
    Result<?> pageStockOut(String outboundNo, String goodsName, String startTime, String endTime,
                           Long pageNum, Long pageSize);

    /** 出库单详情 */
    Result<?> getStockOutDetail(Long id);

    /** 新增出库单（校验可用库存 + 写单 + 扣减库存） */
    Result<?> addStockOut(StockOutAddDTO dto);
}
