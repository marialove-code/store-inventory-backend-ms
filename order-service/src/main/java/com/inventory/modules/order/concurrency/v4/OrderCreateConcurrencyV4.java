package com.inventory.modules.order.concurrency.v4;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * V4：SQL 并发控制（数据库原子条件锁定库存，经 HTTP）。
 * <p>
 * <b>实现要点</b>：
 * <ul>
 *   <li><strong>不用 JVM 锁</strong>：去掉 V2/V3 的 {@code ReentrantLock}</li>
 *   <li><strong>原子 UPDATE</strong>：库存服务 {@code WHERE stock - lock_stock >= buyQty}</li>
 *   <li><strong>远程调用</strong>：{@code InventoryStockClient.lock(goodsId, qty, orderNo)}</li>
 * </ul>
 * </p>
 * <p>
 * <b>压测目标</b>：200 并发下成功订单数 ≤ 100，{@code lockStock <= stock}。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV4 implements OrderCreateConcurrencyStrategy {

    /** V4 同步核心：SQL 原子锁库存 + 保存订单 */
    private final OrderCreateConcurrencyV4SyncService syncService;

    @Override
    public String version() {
        return ConcurrencyVersion.V4.getCode();
    }

    /**
     * V4 创建订单入口。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        try {
            String orderNo = syncService.syncCreateOrderWithSqlLock(dto);
            return Result.success("订单创建成功，库存已锁定（V4：SQL 原子条件，订单号 " + orderNo + "）");
        } catch (IllegalStateException | BusinessException ex) {
            return Result.fail(ex.getMessage());
        }
    }
}
