package com.inventory.modules.invertory.stockwarn.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;
import com.inventory.modules.invertory.stockwarn.dto.StockWarnDTO;

/**
 * 库存预警业务接口
 * 功能：库存预警列表、详情、阈值修改
 */
public interface InventoryWarnService {

    /**
     * 库存预警分页列表查询
     */
    Result<?> pageWarnList(String goodsName, Long pageNum, Long pageSize);

    /**
     * 获取预警商品详情
     */
    Result<?> getWarnDetail(String id);

    /**
     * 修改库存预警阈值
     */
    Result<?> updateStockWarn(String id, StockWarnDTO dto);
}
