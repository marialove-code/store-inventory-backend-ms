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
 * V1 同步核心：复现单体「读可用库存 → 建单 → 非原子 lockStock」基线。
 * <p>
 * <b>为何不直接调 MS 正式 {@code OrderInfoService#createOrder}？</b>
 * 微服务正式建单已改为「先原子 lock（带 bizNo）再 save」，无法复现 V1 超锁。
 * 本类刻意保持与单体相同的顺序与非原子锁，便于压测对照文档。
 * </p>
 * <p>
 * 库存操作一律走 {@link InventoryStockClient}，禁止注入 StockService / Mapper。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderCreateConcurrencyV1SyncService {

    private final OrderInfoService orderInfoService;
    private final InventoryStockClient inventoryStockClient;

    /**
     * V1 基线建单：getUsableStock 校验 → save(order) → lockNonAtomic。
     *
     * @param dto 下单入参
     * @return 成功时返回订单号
     * @throws IllegalStateException 库存不足等业务失败
     */
    @Transactional(rollbackFor = Exception.class)
    public String syncCreateOrderNonAtomic(OrderInfoDTO dto) {
        // ===================== 1. 远程查询可用库存（读快照，非原子） =====================
        int usableStock = inventoryStockClient.getUsableStock(dto.getGoodsId());
        if (usableStock < dto.getBuyQty()) {
            throw new IllegalStateException("库存不足，当前可用库存：" + usableStock);
        }

        // ===================== 2. 构建并保存订单（与单体一致：先落单再锁） =====================
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

        // ===================== 3. 非原子锁定库存（无 bizNo，可复现超锁） =====================
        inventoryStockClient.lockNonAtomic(dto.getGoodsId(), dto.getBuyQty());

        return orderNo;
    }
}
