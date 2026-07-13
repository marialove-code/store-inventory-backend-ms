package com.inventory.modules.order.concurrency.v3;

import cn.hutool.core.bean.BeanUtil;
import com.inventory.common.client.dto.LockStockFlowContext;
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
 * V3 同步核心路径：校验 → 建单 → 仅更新 lock_stock（流水异步）。
 * <p>
 * <b>微服务适配</b>：去掉 InventoryStockMapper / StockService，
 * 读可用库存用 {@link InventoryStockClient#getUsableStock}，
 * 更新锁定用 {@link InventoryStockClient#lockUpdateOnly}。
 * </p>
 * <p>
 * <b> deliberately 不做的事</b>（交给 {@link OrderCreateConcurrencyV3} 的线程池异步执行）：
 * <ul>
 *   <li>写 {@code inventory_flow} 库存流水（远程 writeFlow）</li>
 *   <li>模拟发送下单通知（日志）</li>
 * </ul>
 * </p>
 * <p>
 * 必须与 V2 一样在 {@code ReentrantLock} 内调用，保证不超锁。
 * 注意：本地事务管不到远程库存 HTTP。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderCreateConcurrencyV3SyncService {

    private final InventoryStockClient inventoryStockClient;
    private final OrderInfoService orderInfoService;

    /**
     * V3 同步核心：校验库存、保存订单、仅更新 lock_stock（不写流水）。
     *
     * @param dto 下单入参，与正式接口一致
     * @return 同步成功后的订单号与流水上下文；失败抛业务异常由上层转 Result.fail
     */
    @Transactional(rollbackFor = Exception.class)
    public V3SyncCreateResult syncCreateOrderAndLockStock(OrderInfoDTO dto) {
        // ===================== 1. 远程查询可用库存 =====================
        int usableStock = inventoryStockClient.getUsableStock(dto.getGoodsId());
        if (usableStock < dto.getBuyQty()) {
            throw new IllegalStateException("库存不足，当前可用库存：" + usableStock);
        }

        // ===================== 2. 构建并保存订单 =====================
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

        // ===================== 3. 仅更新 lock_stock（流水异步补写） =====================
        LockStockFlowContext flowContext =
                inventoryStockClient.lockUpdateOnly(dto.getGoodsId(), dto.getBuyQty());

        return V3SyncCreateResult.builder()
                .orderNo(orderNo)
                .userName(dto.getUserName())
                .flowContext(flowContext)
                .build();
    }
}
