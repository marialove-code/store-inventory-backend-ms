package com.inventory.modules.invertory.stock.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.inventory.common.client.dto.LockStockFlowContext;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.invertory.stockflow.entity.InventoryFlow;
import com.inventory.modules.invertory.stockflow.mapper.InventoryFlowMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 库存核心业务实现类（从单体完整迁入）。
 * <p>
 * 功能说明：
 * <ol>
 *   <li>统一封装所有库存操作：增加、扣减、锁定、释放</li>
 *   <li>所有操作带事务，异常自动回滚</li>
 *   <li>自动记录库存流水，便于审计与排查</li>
 *   <li>自动维护库存状态：正常/预警/缺货</li>
 * </ol>
 * 注意：本类禁止随意修改逻辑，所有库存变更必须走这里。
 * </p>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class StockServiceImpl implements StockService {

    /** 库存实时表 Mapper */
    @Resource
    private InventoryStockMapper inventoryStockMapper;

    /** 库存流水表 Mapper */
    @Resource
    private InventoryFlowMapper inventoryFlowMapper;

    // ======================== 1. 增加库存（入库 / 退货回库） ========================

    /**
     * 增加可用库存。
     * 场景：采购入库、退货审核通过回库。
     */
    @Override
    public void increaseStock(Long goodsId, Integer qty) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        // 执行 SQL：stock = stock + qty
        updateStockWrapper.setSql("stock = stock + " + qty);
        updateStockWrapper.set(InventoryStock::getLastReceiptTime, LocalDateTime.now());
        updateStockWrapper.set(InventoryStock::getOperateType, 1);
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);

        inventoryStockMapper.update(null, updateStockWrapper);

        Integer operateType = 1;
        String remark = "入库增加库存";
        writeFlow(
                goodsId,
                stock.getGoodsName(),
                stock.getStock(),
                qty,
                stock.getStock() + qty,
                operateType,
                null,
                remark
        );

        refreshStatus(goodsId);
    }

    @Override
    public void increaseStockFlow(Long goodsId, Integer qty, String receiptNo) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateStockWrapper.setSql("stock = stock + " + qty);
        updateStockWrapper.set(InventoryStock::getLastReceiptTime, LocalDateTime.now());
        updateStockWrapper.set(InventoryStock::getOperateType, 1);
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);

        inventoryStockMapper.update(null, updateStockWrapper);

        Integer operateType = 1;
        String remark = "入库增加库存";
        writeFlow(
                goodsId,
                stock.getGoodsName(),
                stock.getStock(),
                qty,
                stock.getStock() + qty,
                operateType,
                receiptNo,
                remark
        );

        refreshStatus(goodsId);
    }

    // ======================== 2. 扣减库存（发货 / 出库） ========================

    /**
     * 扣减可用库存。
     * 场景：订单发货、手动出库。
     */
    @Override
    public void decreaseStock(Long goodsId, Integer qty) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateStockWrapper.setSql("stock = stock - " + qty);
        updateStockWrapper.set(InventoryStock::getLastReceiptTime, LocalDateTime.now());
        updateStockWrapper.set(InventoryStock::getOperateType, 2);
        inventoryStockMapper.update(null, updateStockWrapper);

        Integer operateType = 2;
        String remark = "出库扣减库存";
        writeFlow(
                goodsId,
                stock.getGoodsName(),
                stock.getStock(),
                -qty,
                stock.getStock() - qty,
                operateType,
                null,
                remark
        );

        refreshStatus(goodsId);
    }

    @Override
    public void decreaseStockFlow(Long goodsId, Integer qty, String receiptNo) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateStockWrapper.setSql("stock = stock - " + qty);
        updateStockWrapper.set(InventoryStock::getLastReceiptTime, LocalDateTime.now());
        updateStockWrapper.set(InventoryStock::getOperateType, 2);
        // 发货：同时释放对应锁定数量
        updateStockWrapper.set(InventoryStock::getLockStock, stock.getLockStock() - qty);
        inventoryStockMapper.update(null, updateStockWrapper);

        Integer operateType = 2;
        String remark = "出库扣减库存";
        writeFlow(
                goodsId,
                stock.getGoodsName(),
                stock.getStock(),
                -qty,
                stock.getStock() - qty,
                operateType,
                receiptNo,
                remark
        );

        refreshStatus(goodsId);
    }

    // ======================== 3. 锁定库存（创建订单预占） ========================

    /**
     * 锁定库存（订单预占）。
     * 场景：用户创建订单，库存预锁定，防止超卖。
     */
    @Override
    public void lockStock(Long goodsId, Integer qty) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateStockWrapper.setSql("lock_stock = lock_stock + " + qty);
        inventoryStockMapper.update(null, updateStockWrapper);

        Integer operateType = 3;
        String remark = "订单预占锁定库存";
        writeFlow(
                goodsId,
                stock.getGoodsName(),
                stock.getLockStock(),
                qty,
                stock.getLockStock() + qty,
                operateType,
                null,
                remark
        );
    }

    /**
     * V3：仅更新 lock_stock，不写流水（流水在线程池中异步补写）。
     */
    @Override
    public LockStockFlowContext lockStockUpdateOnly(Long goodsId, Integer qty) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        int before = stock.getLockStock() == null ? 0 : stock.getLockStock();
        int after = before + qty;

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateStockWrapper.setSql("lock_stock = lock_stock + " + qty);
        inventoryStockMapper.update(null, updateStockWrapper);

        return LockStockFlowContext.builder()
                .goodsId(goodsId)
                .goodsName(stock.getGoodsName())
                .beforeLockStock(before)
                .changeQty(qty)
                .afterLockStock(after)
                .build();
    }

    /**
     * V4：在数据库层用「带条件的 UPDATE」原子锁定库存，并同步写流水。
     * <p>
     * 核心 SQL 见 {@link InventoryStockMapper#lockStockIfAvailable}：
     * {@code WHERE stock - lock_stock >= qty}，避免 V1 读-改-写竞态。
     * </p>
     */
    @Override
    public LockStockFlowContext lockStockAtomically(Long goodsId, Integer qty, String orderNo) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        if (stock == null) {
            throw new IllegalStateException("商品库存不存在");
        }

        int beforeLockStock = stock.getLockStock() == null ? 0 : stock.getLockStock();

        int affectedRows = inventoryStockMapper.lockStockIfAvailable(goodsId, qty);
        if (affectedRows == 0) {
            int totalStock = stock.getStock() == null ? 0 : stock.getStock();
            int usableStock = totalStock - beforeLockStock;
            throw new IllegalStateException("库存不足，当前可用库存：" + usableStock);
        }

        InventoryStock updated = inventoryStockMapper.selectOne(queryStockWrapper);
        int afterLockStock = updated.getLockStock() == null ? 0 : updated.getLockStock();
        int beforeForFlow = afterLockStock - qty;

        writeFlow(
                goodsId,
                updated.getGoodsName(),
                beforeForFlow,
                qty,
                afterLockStock,
                3,
                orderNo,
                "V4 SQL原子锁定：订单预占锁定库存"
        );

        return LockStockFlowContext.builder()
                .goodsId(goodsId)
                .goodsName(updated.getGoodsName())
                .beforeLockStock(beforeForFlow)
                .changeQty(qty)
                .afterLockStock(afterLockStock)
                .build();
    }

    // ======================== 4. 释放库存（取消订单） ========================

    /**
     * 释放已锁定的库存。
     * 场景：订单取消、订单关闭。
     */
    @Override
    public void unlockStock(Long goodsId, Integer qty) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        LambdaUpdateWrapper<InventoryStock> updateStockWrapper = new LambdaUpdateWrapper<>();
        updateStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateStockWrapper.setSql("lock_stock = lock_stock - " + qty);
        inventoryStockMapper.update(null, updateStockWrapper);

        Integer operateType = 4;
        String remark = "取消订单释放库存";
        writeFlow(
                goodsId,
                stock.getGoodsName(),
                stock.getLockStock(),
                -qty,
                stock.getLockStock() - qty,
                operateType,
                null,
                remark
        );
    }

    // ======================== 5. 统一写入库存流水 ========================

    /**
     * 统一记录库存流水日志。
     * 所有库存变动必须调用此方法，保证审计完整。
     * 主键使用 Hutool 雪花 ID。
     */
    @Override
    public void writeFlow(
            Long goodsId,
            String goodsName,
            Integer beforeStock,
            Integer changeStock,
            Integer afterStock,
            Integer operateType,
            String bizNo,
            String remark
    ) {
        InventoryFlow flow = new InventoryFlow();

        // 雪花 ID 生成主键
        flow.setId(IdUtil.getSnowflakeNextId());

        flow.setGoodsId(goodsId);
        flow.setGoodsName(goodsName);

        flow.setBeforeStock(beforeStock);
        flow.setChangeStock(changeStock);
        flow.setAfterStock(afterStock);

        flow.setOperateType(operateType);
        flow.setOperator("system");
        flow.setRemark(remark);

        flow.setBizNo(bizNo);

        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        flow.setSort(0);

        inventoryFlowMapper.insert(flow);
    }

    // ======================== 6. 自动刷新库存状态 ========================

    /**
     * 自动计算并更新库存状态：1=正常，2=预警，3=缺货。
     */
    @Override
    public void refreshStatus(Long goodsId) {
        LambdaQueryWrapper<InventoryStock> queryStockWrapper = new LambdaQueryWrapper<>();
        queryStockWrapper.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(queryStockWrapper);

        Integer status;
        if (stock.getStock() <= 0) {
            status = 3;
        } else if (stock.getStock() < stock.getStockWarn()) {
            status = 2;
        } else {
            status = 1;
        }

        LambdaUpdateWrapper<InventoryStock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(InventoryStock::getGoodsId, goodsId);
        updateWrapper.set(InventoryStock::getStockStatus, status);
        inventoryStockMapper.update(null, updateWrapper);
    }
}
