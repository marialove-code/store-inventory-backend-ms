package com.inventory.modules.invertory.stockout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;

/**
 * 出库单 Mapper，对应表 {@code inventory_out}。
 */
public interface InventoryOutMapper extends BaseMapper<InventoryOut> {

    /**
     * 查询当前最大 sort（不含 +1），由业务层自行 +1。
     */
    Integer selectMaxSort();
}
