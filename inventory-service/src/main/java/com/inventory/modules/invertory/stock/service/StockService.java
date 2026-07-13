package com.inventory.modules.invertory.stock.service;

import com.inventory.common.client.dto.LockStockFlowContext;

/**
 * 核心库存业务服务。
 * <p>
 * 统一封装：库存增加、扣减、锁定、释放、流水记录、状态刷新。
 * 所有业务模块（入库、出库、订单、退货）都应通过本接口操作库存，禁止直接改表。
 * </p>
 */
public interface StockService {

    /**
     * 增加库存（入库、退货回库使用）。
     *
     * @param goodsId 商品 ID
     * @param qty     增加数量
     */
    void increaseStock(Long goodsId, Integer qty);

    /**
     * 增加库存并关联业务单号（写流水 bizNo）。
     *
     * @param goodsId   商品 ID
     * @param qty       增加数量
     * @param receiptNo 业务单号（如入库单号）
     */
    void increaseStockFlow(Long goodsId, Integer qty, String receiptNo);

    /**
     * 扣减库存（发货、出库使用）。
     *
     * @param goodsId 商品 ID
     * @param qty     扣减数量
     */
    void decreaseStock(Long goodsId, Integer qty);

    /**
     * 扣减库存并关联业务单号；同时减少 lock_stock（订单发货场景）。
     *
     * @param goodsId   商品 ID
     * @param qty       扣减数量
     * @param receiptNo 业务单号（如物流单号）
     */
    void decreaseStockFlow(Long goodsId, Integer qty, String receiptNo);

    /**
     * 锁定库存（创建订单预占）。
     *
     * @param goodsId 商品 ID
     * @param qty     锁定数量
     */
    void lockStock(Long goodsId, Integer qty);

    /**
     * V3 专用：仅执行 {@code lock_stock = lock_stock + qty}，<strong>不</strong>写库存流水。
     * <p>
     * 流水由 V3 线程池异步调用 {@link #writeFlow}，以缩短同步路径持有锁/事务的时间。
     * V1/V2 仍使用 {@link #lockStock}（同步写流水）。
     * </p>
     *
     * @return 异步写流水所需的 before/after 等上下文
     */
    LockStockFlowContext lockStockUpdateOnly(Long goodsId, Integer qty);

    /**
     * V4：SQL 原子条件锁定库存（一条 UPDATE + WHERE 可用库存充足）。
     * <p>
     * 不使用 JVM 锁；依赖数据库对单行 UPDATE 的原子性。
     * 成功时同步写入库存流水；失败抛 {@link IllegalStateException}。
     * </p>
     *
     * @param goodsId 商品 ID
     * @param qty     锁定数量
     * @param orderNo 关联订单号（写流水）
     * @return 流水上下文（便于上层日志扩展）
     */
    LockStockFlowContext lockStockAtomically(Long goodsId, Integer qty, String orderNo);

    /**
     * 释放锁定库存（取消订单）。
     *
     * @param goodsId 商品 ID
     * @param qty     释放数量
     */
    void unlockStock(Long goodsId, Integer qty);

    /**
     * 统一写入库存流水（所有库存变动必须记录）。
     *
     * @param goodsId     商品 ID
     * @param goodsName   商品名称
     * @param beforeStock 变动前库存
     * @param changeStock 变动数量（正数增加，负数减少）
     * @param afterStock  变动后库存
     * @param operateType 操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整
     * @param bizNo       业务单号（入库单号/订单号/退货单号）
     * @param remark      备注
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
     * 刷新并更新商品库存状态（正常/预警/缺货）。
     * 每次库存增减后自动执行。
     *
     * @param goodsId 商品 ID
     */
    void refreshStatus(Long goodsId);
}
