package com.inventory.modules.invertory.stockout.mapper;

import com.inventory.modules.invertory.stockout.entity.InventoryOut;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author 95349
* @description 针对表【inventory_out】的数据库操作Mapper
* @createDate 2026-05-29 19:04:18
* @Entity com.inventory.modules.invertory.stockout.entity.InventoryOut
*/
public interface InventoryOutMapper extends BaseMapper<InventoryOut> {
    /**
     * 查询最大序号
     * @return
     */
    Integer selectMaxSort();
}




