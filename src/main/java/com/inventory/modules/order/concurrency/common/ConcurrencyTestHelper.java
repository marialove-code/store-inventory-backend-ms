package com.inventory.modules.order.concurrency.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.order.concurrency.common.vo.ConcurrencyStockResultVO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 并发压测辅助工具。
 * <p>
 * 职责：压测前重置库存、压测后查询 stock / lockStock / 待支付订单数，便于填写 {@code docs/并发演进.md}。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ConcurrencyTestHelper {

    private final InventoryStockMapper inventoryStockMapper;
    private final OrderInfoMapper orderInfoMapper;

    /**
     * 重置指定商品的库存状态，供下一轮压测使用。
     * <p>
     * 注意：本方法仅更新 {@code inventory_stock} 表的 stock / lockStock，
     * 不会自动删除历史测试订单；若需干净环境请手动清理订单或后续扩展此方法。
     * </p>
     *
     * @param goodsId   商品 ID
     * @param stock     重置后的当前库存
     * @param lockStock 重置后的锁定库存，一般为 0
     * @return 操作结果
     */
    public Result<Void> resetStock(Long goodsId, Integer stock, Integer lockStock) {
        if (goodsId == null) {
            return Result.fail("goodsId 不能为空");
        }
        if (stock == null || stock < 0) {
            return Result.fail("stock 无效");
        }
        if (lockStock == null || lockStock < 0) {
            return Result.fail("lockStock 无效");
        }

        LambdaUpdateWrapper<InventoryStock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(InventoryStock::getGoodsId, goodsId)
                .set(InventoryStock::getStock, stock)
                .set(InventoryStock::getLockStock, lockStock);

        int rows = inventoryStockMapper.update(null, updateWrapper);
        if (rows == 0) {
            return Result.fail("未找到该商品的库存记录，goodsId=" + goodsId);
        }
        return Result.success();
    }

    /**
     * 查询指定商品当前库存快照，用于压测结果核对。
     *
     * @param goodsId 商品 ID
     * @return 库存快照；商品不存在时 fail
     */
    public Result<ConcurrencyStockResultVO> queryStockResult(Long goodsId) {
        if (goodsId == null) {
            return Result.fail("goodsId 不能为空");
        }

        LambdaQueryWrapper<InventoryStock> stockQuery = new LambdaQueryWrapper<>();
        stockQuery.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stockEntity = inventoryStockMapper.selectOne(stockQuery);
        if (stockEntity == null) {
            return Result.fail("商品库存不存在，goodsId=" + goodsId);
        }

        int stock = stockEntity.getStock() == null ? 0 : stockEntity.getStock();
        int lockStock = stockEntity.getLockStock() == null ? 0 : stockEntity.getLockStock();
        int usableStock = stock - lockStock;

        // 统计该商品待支付订单数（与锁定库存对照）
        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(OrderInfo::getGoodsId, goodsId)
                .eq(OrderInfo::getOrderStatus, OrderStatusEnum.PENDING_PAYMENT.getCode());
        Long pendingCount = orderInfoMapper.selectCount(orderQuery);

        boolean overLocked = lockStock > stock || usableStock < 0;

        ConcurrencyStockResultVO vo = ConcurrencyStockResultVO.builder()
                .goodsId(goodsId)
                .goodsName(stockEntity.getGoodsName())
                .stock(stock)
                .lockStock(lockStock)
                .usableStock(usableStock)
                .pendingOrderCount(pendingCount)
                .overLocked(overLocked)
                .build();

        return Result.success(vo);
    }
}
