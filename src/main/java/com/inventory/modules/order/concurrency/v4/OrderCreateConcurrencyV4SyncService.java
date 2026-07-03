package com.inventory.modules.order.concurrency.v4;

import cn.hutool.core.bean.BeanUtil;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V4 同步核心：在一个事务内完成「SQL 原子锁库存 → 保存订单」。
 * <p>
 * <b>与 V2/V3 的区别</b>：不再使用 {@code ReentrantLock}，并发安全交给数据库
 * {@code UPDATE ... WHERE stock - lock_stock >= qty}。
 * </p>
 * <p>
 * <b>顺序说明</b>：先生成订单号并执行原子锁库存（失败则直接抛异常，不插入订单），
 * 再 {@code save(order)}，避免产生无库存支撑的订单行。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderCreateConcurrencyV4SyncService {

    private final StockService stockService;
    private final OrderInfoService orderInfoService;

    /**
     * V4 创建订单并锁定库存（SQL 原子条件）。
     *
     * @param dto 下单入参
     * @return 生成的订单号
     * @throws IllegalStateException 商品不存在或库存不足（由 {@link StockService#lockStockAtomically} 抛出）
     */
    @Transactional(rollbackFor = Exception.class)
    public String syncCreateOrderWithSqlLock(OrderInfoDTO dto) {
        // ===================== 1. 预生成订单号（原子锁库存写流水时需要关联单号） =====================
        String orderNo = OrderNoGenerator.generate(OrderPrefix.ORDER);

        // ===================== 2. SQL 原子锁定 + 同步写流水（核心：无 JVM 锁） =====================
        // 若可用库存不足，UPDATE 影响行数为 0，抛出「库存不足」
        stockService.lockStockAtomically(dto.getGoodsId(), dto.getBuyQty(), orderNo);

        // ===================== 3. 构建订单实体 =====================
        OrderInfo order = new OrderInfo();
        BeanUtil.copyProperties(dto, order);
        order.setOrderNo(orderNo);

        BigDecimal orderAmount = dto.getSalePrice().multiply(BigDecimal.valueOf(dto.getBuyQty()));
        order.setOrderAmount(orderAmount);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // ===================== 4. 保存订单（与步骤 2 同一事务，任一步失败整体回滚） =====================
        orderInfoService.save(order);

        return orderNo;
    }
}
