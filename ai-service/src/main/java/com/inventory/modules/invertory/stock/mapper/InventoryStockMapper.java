package com.inventory.modules.invertory.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.invertory.stock.entity.InventoryStock;

/**
 * 库存表 Mapper（ai-service 精简版：补货建议 + 看板统计）。
 * <p>只保留 BaseMapper 与看板用到的聚合查询，不含锁库存命令。</p>
 */
public interface InventoryStockMapper extends BaseMapper<InventoryStock> {

    /** 库存总量 */
    Integer getTotalStock();

    /** 库存预警商品数量 */
    Integer getWarnStockCount();
}
