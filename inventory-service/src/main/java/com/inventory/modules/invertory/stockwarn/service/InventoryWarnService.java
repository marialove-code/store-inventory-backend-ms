package com.inventory.modules.invertory.stockwarn.service;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockwarn.dto.StockWarnDTO;

/**
 * 库存预警业务接口。
 */
public interface InventoryWarnService {

    /** 预警分页列表：stock &lt; stock_warn */
    Result<?> pageWarnList(String goodsName, Long pageNum, Long pageSize);

    /** 预警商品详情 */
    Result<?> getWarnDetail(String id);

    /** 修改预警阈值 */
    Result<?> updateStockWarn(String id, StockWarnDTO dto);
}
