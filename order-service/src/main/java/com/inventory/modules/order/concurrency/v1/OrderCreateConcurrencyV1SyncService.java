package com.inventory.modules.order.concurrency.v1;

import cn.hutool.core.bean.BeanUtil;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import com.inventory.order.client.InventoryStockClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1 同步核心：临界区内的三步业务（V2 / V5 / V5r 持锁后都调用这里）。
 * <p>
 * 【先理解这个，再理解锁】<br>
 * 没有外层锁时（纯 V1），多线程会在「步骤 1 读库存」和「步骤 3 锁库存」之间插队 → 超锁。<br>
 * V2/V5 做的事：保证同一商品同一时刻只有一个线程能跑完下面 1→2→3。
 * </p>
 * <p>
 * 【超锁时间线（无锁时）】
 * <pre>
 * 线程 A：读到可用=1  ──┐
 * 线程 B：读到可用=1  ──┤ 都觉得「还有 1 件」
 * 线程 A：建单 + lockStock+1
 * 线程 B：建单 + lockStock+1  → 锁了 2 次，超锁
 * </pre>
 * </p>
 * <p>
 * 库存一律走 {@link InventoryStockClient}（HTTP 调 inventory-service），不直接改库。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderCreateConcurrencyV1SyncService {

    private final OrderInfoService orderInfoService;
    private final InventoryStockClient inventoryStockClient;

    /**
     * 非原子建单三步曲（读代码时按 1→2→3 跟）。
     *
     * @return 订单号
     * @throws IllegalStateException 库存不足等
     */
    @Transactional(rollbackFor = Exception.class)
    public String syncCreateOrderNonAtomic(OrderInfoDTO dto) {
        // ========== 步骤 1：远程读「当前可用库存」快照 ==========
        // 注意：这只是某一瞬间的读数，读完到步骤 3 之间若无外层锁，别人也能读到同样的数
        int usableStock = inventoryStockClient.getUsableStock(dto.getGoodsId());
        if (usableStock < dto.getBuyQty()) {
            throw new IllegalStateException("库存不足，当前可用库存：" + usableStock);
        }

        // ========== 步骤 2：先落订单（待支付）==========
        // 与单体压测基线一致：先 save 订单，再去锁库存
        OrderInfo order = new OrderInfo();
        BeanUtil.copyProperties(dto, order);

        String orderNo = OrderNoGenerator.generate(OrderPrefix.ORDER);
        order.setOrderNo(orderNo);

        BigDecimal orderAmount = dto.getSalePrice().multiply(BigDecimal.valueOf(dto.getBuyQty()));
        order.setOrderAmount(orderAmount);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderInfoService.save(order);

        // ========== 步骤 3：非原子锁定库存 ==========
        // lockNonAtomic：库存服务侧「加 lock_stock」，没有「可用>=qty」那种原子 WHERE（那是 V4）
        // 无外层互斥时，两线程都能通过步骤 1，再各执行一次本步骤 → 超锁
        inventoryStockClient.lockNonAtomic(dto.getGoodsId(), dto.getBuyQty());

        return orderNo;
    }
}
