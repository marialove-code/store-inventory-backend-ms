package com.inventory.modules.order.concurrency.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.vo.ConcurrencyStockResultVO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import com.inventory.order.client.InventoryStockClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 并发压测辅助工具（微服务适配版）。
 * <p>
 * 职责：压测前重置库存、压测后查询 stock / lockStock / 待支付订单数，便于填写 {@code docs/并发/01-压测数据.md}。
 * <b>库存读写一律通过 {@link InventoryStockClient} HTTP 调用，禁止注入 InventoryStockMapper。</b>
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ConcurrencyTestHelper {

    /** 库存远程客户端（重置库存、查询快照） */
    private final InventoryStockClient inventoryStockClient;

    /** 本地订单 Mapper（统计待支付订单数） */
    private final OrderInfoMapper orderInfoMapper;

    /**
     * 重置指定商品的库存状态，供下一轮压测使用。
     * <p>
     * 通过库存服务 {@code /inventory/internal/dev/reset-stock} 更新 stock / lockStock；
     * 不会自动删除历史测试订单。
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

        try {
            inventoryStockClient.resetStock(goodsId, stock, lockStock);
            return Result.success();
        } catch (BusinessException ex) {
            return Result.fail(ex.getCode(), ex.getMessage());
        }
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

        Map<String, Object> snapshot;
        try {
            snapshot = inventoryStockClient.getUsable(goodsId);
        } catch (BusinessException ex) {
            return Result.fail(ex.getCode(), ex.getMessage());
        }

        if (snapshot == null) {
            return Result.fail("商品库存不存在，goodsId=" + goodsId);
        }

        int stock = toInt(snapshot.get("stock"));
        int lockStock = toInt(snapshot.get("lockStock"));
        int usableStock = toInt(snapshot.get("usableStock"));
        String goodsName = snapshot.get("goodsName") != null
                ? String.valueOf(snapshot.get("goodsName"))
                : null;

        // 统计该商品待支付订单数（与锁定库存对照，仍走本地订单表）
        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(OrderInfo::getGoodsId, goodsId)
                .eq(OrderInfo::getOrderStatus, OrderStatusEnum.PENDING_PAYMENT.getCode());
        Long pendingCount = orderInfoMapper.selectCount(orderQuery);

        boolean overLocked = lockStock > stock || usableStock < 0;

        ConcurrencyStockResultVO vo = ConcurrencyStockResultVO.builder()
                .goodsId(goodsId)
                .goodsName(goodsName)
                .stock(stock)
                .lockStock(lockStock)
                .usableStock(usableStock)
                .pendingOrderCount(pendingCount)
                .overLocked(overLocked)
                .build();

        return Result.success(vo);
    }

    /**
     * 将快照字段安全转为 int。
     */
    private static int toInt(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        return Integer.parseInt(String.valueOf(raw));
    }
}
