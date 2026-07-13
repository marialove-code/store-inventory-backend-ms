package com.inventory.modules.order.concurrency.v4;

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
 * V4 同步核心：SQL 原子锁库存 → 保存订单。
 * <p>
 * <b>微服务适配</b>：{@code lockStockAtomically} 改为
 * {@link InventoryStockClient#lock(Long, Integer, String)}（带 orderNo 作为 bizNo），
 * 库存侧内部走原子 UPDATE。
 * </p>
 * <p>
 * <b>顺序说明</b>：先生成订单号并远程原子锁库存（失败则不插入订单），
 * 再 {@code save(order)}。本地事务管不到远程库存。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderCreateConcurrencyV4SyncService {

    private final InventoryStockClient inventoryStockClient;
    private final OrderInfoService orderInfoService;

    /**
     * V4 创建订单并锁定库存（SQL 原子条件，经 HTTP）。
     *
     * @param dto 下单入参
     * @return 生成的订单号
     * @throws com.inventory.common.exception.BusinessException 商品不存在或库存不足
     */
    @Transactional(rollbackFor = Exception.class)
    public String syncCreateOrderWithSqlLock(OrderInfoDTO dto) {
        // ===================== 1. 预生成订单号（原子锁库存写流水时需要关联单号） =====================
        String orderNo = OrderNoGenerator.generate(OrderPrefix.ORDER);

        // ===================== 2. 远程 SQL 原子锁定 + 同步写流水（无 JVM 锁） =====================
        inventoryStockClient.lock(dto.getGoodsId(), dto.getBuyQty(), orderNo);

        // ===================== 3. 构建订单实体 =====================
        OrderInfo order = new OrderInfo();
        BeanUtil.copyProperties(dto, order);
        order.setOrderNo(orderNo);

        BigDecimal orderAmount = dto.getSalePrice().multiply(BigDecimal.valueOf(dto.getBuyQty()));
        order.setOrderAmount(orderAmount);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // ===================== 4. 保存订单；失败时尝试远程解锁补偿 =====================
        try {
            orderInfoService.save(order);
        } catch (Exception ex) {
            try {
                inventoryStockClient.unlock(dto.getGoodsId(), dto.getBuyQty());
            } catch (Exception ignored) {
                // 补偿失败留给后续对账；此处不吞建单异常
            }
            throw ex;
        }

        return orderNo;
    }
}
