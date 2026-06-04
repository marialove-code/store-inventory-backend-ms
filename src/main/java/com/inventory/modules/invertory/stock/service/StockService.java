package com.inventory.modules.invertory.stock.service;

/**
 * 核心库存业务服务
 * 统一封装：库存增加、扣减、锁定、释放、流水记录、状态刷新
 * 所有业务模块（入库、出库、订单、退货）都调用此类操作库存
 */
public interface StockService {

    /**
     * 增加库存（入库、退货回库 使用）
     * @param goodsId 商品ID
     * @param qty 增加的数量
     */
    void increaseStock(Long goodsId, Integer qty);

    /**
     * 扣减库存（发货、出库 使用）
     * @param goodsId 商品ID
     * @param qty 扣减的数量
     */
    void decreaseStock(Long goodsId, Integer qty);

    /**
     * 锁定库存（创建订单预占库存 使用）
     * @param goodsId 商品ID
     * @param qty 锁定数量
     */
    void lockStock(Long goodsId, Integer qty);

    /**
     * 释放锁定库存（取消订单 释放库存 使用）
     * @param goodsId 商品ID
     * @param qty 释放数量
     */
    void unlockStock(Long goodsId, Integer qty);

    /**
     * 统一写入库存流水（所有库存变动必须记录）
     * @param goodsId      商品ID
     * @param goodsName    商品名称
     * @param beforeStock  变动前库存
     * @param changeStock  变动数量（正数增加，负数减少）
     * @param afterStock   变动后库存
     * @param operateType  操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整
     * @param bizNo        业务单号（入库单号/订单号/退货单号）
     * @param remark       备注
     */
    void writeFlow(
            Long goodsId,
            String goodsName,
            Integer beforeStock,
            Integer changeStock,
            Integer afterStock,
            Integer operateType,
            String bizNo,
            String remark
    );

    /**
     * 刷新并更新商品库存状态（正常/预警/缺货）
     * 每次库存变动后自动执行
     * @param goodsId 商品ID
     */
    void refreshStatus(Long goodsId);
}