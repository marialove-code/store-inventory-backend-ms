package com.inventory.modules.invertory.stock.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 95349
* @description 针对表【inventory_stock】的数据库操作Mapper
* @createDate 2026-05-29 19:04:18
* @Entity com.inventory.modules.invertory.stock.entity.InventoryStock
*/
public interface InventoryStockMapper extends BaseMapper<InventoryStock> {


    // 库存总量
    Integer getTotalStock();

    // 库存预警商品数量
    Integer getWarnStockCount();
}




