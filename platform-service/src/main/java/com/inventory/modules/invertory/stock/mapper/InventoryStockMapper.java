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

    /**
     * V4：原子锁定库存。
     * <p>
     * 仅当 {@code stock - lock_stock >= qty} 时执行 {@code lock_stock = lock_stock + qty}，
     * 利用数据库单行 UPDATE 的原子性防止超锁。
     * </p>
     *
     * @param goodsId 商品 ID
     * @param qty     锁定数量
     * @return 影响行数：1 表示成功，0 表示库存不足或商品不存在
     */
    int lockStockIfAvailable(@Param("goodsId") Long goodsId, @Param("qty") Integer qty);
}
