package com.inventory.modules.invertory.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import org.apache.ibatis.annotations.Param;

/**
 * 库存实时表 Mapper，对应表 {@code inventory_stock}。
 * <p>
 * 除 BaseMapper CRUD 外，提供统计与 V4 原子锁定 SQL（见 XML）。
 * namespace 必须与 {@code InventoryStockMapper.xml} 保持一致。
 * </p>
 */
public interface InventoryStockMapper extends BaseMapper<InventoryStock> {

    /** 库存总量（全表 SUM(stock)） */
    Integer getTotalStock();

    /** 库存预警商品数量（stock &lt;= stock_warn） */
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
