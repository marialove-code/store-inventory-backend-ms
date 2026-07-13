package com.inventory.modules.order.orderinfo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;
import com.inventory.modules.order.orderdelivery.mapper.OrderDeliveryMapper;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import com.inventory.modules.order.orderinfo.vo.OrderListVO;
import com.inventory.order.client.InventoryStockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务 Service 实现（微服务改造版）。
 * <p>
 * <b>相对单体的核心变化：</b>
 * <ul>
 *   <li>删除 InventoryStockMapper、StockService 注入</li>
 *   <li>库存读写一律通过 {@link InventoryStockClient} HTTP 调库存服务</li>
 * </ul>
 * </p>
 * <p>
 * <b>分布式事务风险（本阶段接受，后续补偿）：</b>
 * 本地 DB 事务无法覆盖远程库存调用。若「先 save 再 lock」，lock 失败虽可回滚订单，
 * 但若 lock 成功、本地事务随后失败，会出现「库存已锁、订单不存在」。
 * 本实现采用更稳妥的临时方案：<b>先 remote lock（带预生成 orderNo），成功后再 save</b>；
 * 若 save 失败则尝试 remote unlock 补偿（简单 try，失败只打日志）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private final OrderInfoMapper orderInfoMapper;

    /** 发货单 Mapper（支付成功时自动生成待发货单） */
    private final OrderDeliveryMapper orderDeliveryMapper;

    /** 库存远程客户端（禁止再注入 StockService / InventoryStockMapper） */
    private final InventoryStockClient inventoryStockClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ======================== 1. 订单分页列表查询 ========================

    @Override
    public Result<?> pageOrderList(String orderNo, String goodsName, String orderStatus,
                                   String startTime, String endTime, Long pageNum, Long pageSize) {
        Long finalPageNum = pageNum == null ? 1L : pageNum;
        Long finalPageSize = pageSize == null ? 10L : pageSize;

        Page<OrderInfo> page = new Page<>(finalPageNum, finalPageSize);
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();

        if (StrUtil.isNotBlank(orderNo)) {
            queryWrapper.eq(OrderInfo::getOrderNo, orderNo.trim());
        }
        if (StrUtil.isNotBlank(goodsName)) {
            queryWrapper.like(OrderInfo::getGoodsName, goodsName.trim());
        }
        if (StrUtil.isNotBlank(orderStatus)) {
            try {
                queryWrapper.eq(OrderInfo::getOrderStatus, Integer.parseInt(orderStatus.trim()));
            } catch (NumberFormatException e) {
                return Result.fail("订单状态格式错误");
            }
        }
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime.trim(), DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endTime.trim(), DATE_TIME_FORMATTER);
                queryWrapper.between(OrderInfo::getCreateTime, start, end);
            } catch (Exception e) {
                return Result.fail("时间格式错误：yyyy-MM-dd HH:mm:ss");
            }
        }

        queryWrapper.orderByDesc(OrderInfo::getCreateTime);
        Page<OrderInfo> orderPage = orderInfoMapper.selectPage(page, queryWrapper);

        List<OrderListVO> voList = new ArrayList<>();
        for (OrderInfo order : orderPage.getRecords()) {
            OrderListVO vo = new OrderListVO();
            BeanUtil.copyProperties(order, vo);
            OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(order.getOrderStatus());
            vo.setStatusName(statusEnum == null ? "未知状态" : statusEnum.getDesc());
            voList.add(vo);
        }

        Page<OrderListVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    // ======================== 2. 创建订单（核心：先锁库存再落单） ========================

    /**
     * 创建订单。
     * <p>
     * <b>推荐顺序（本阶段实现）：</b>
     * <ol>
     *   <li>远程查可用库存（快速失败，减少无效 lock）</li>
     *   <li>预生成 orderNo</li>
     *   <li>remote lock（bizNo=orderNo）；失败则不落订单</li>
     *   <li>save 订单；若 save 失败则尝试 remote unlock 补偿</li>
     * </ol>
     * 对比「先 save 再 lock」：后者在 lock 失败时本地事务可回滚订单，
     * 但若 lock 已成功而事务因其他原因回滚，库存侧可能已锁成功——分布式风险本阶段同样存在，
     * 先 lock 再 save 可避免「有订单无锁」；「有锁无订单」靠 unlock 补偿尽量收敛。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createOrder(OrderInfoDTO dto) {
        // 1. 远程查询可用库存（库存不存在或网络失败会抛 BusinessException）
        int usableStock = inventoryStockClient.getUsableStock(dto.getGoodsId());
        if (usableStock < dto.getBuyQty()) {
            return Result.fail("库存不足，当前可用库存：" + usableStock);
        }

        // 2. 预生成订单号（作为库存锁定的 bizNo，便于对账与原子锁）
        String orderNo = OrderNoGenerator.generate(OrderPrefix.ORDER);

        // 3. 先远程锁库存；失败则不建单（异常向上抛，不吞）
        inventoryStockClient.lock(dto.getGoodsId(), dto.getBuyQty(), orderNo);

        // 4. 构建并保存订单；失败时尝试解锁补偿
        try {
            OrderInfo order = new OrderInfo();
            BeanUtil.copyProperties(dto, order);
            order.setOrderNo(orderNo);

            BigDecimal orderAmount = dto.getSalePrice().multiply(BigDecimal.valueOf(dto.getBuyQty()));
            order.setOrderAmount(orderAmount);
            order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            save(order);
        } catch (Exception ex) {
            // 本地落库失败：尽力解锁，避免长期占库存（补偿失败仅打日志，后续可接对账任务）
            log.error("【建单】订单落库失败，尝试远程解锁补偿 orderNo={}, goodsId={}, qty={}",
                    orderNo, dto.getGoodsId(), dto.getBuyQty(), ex);
            try {
                inventoryStockClient.unlock(dto.getGoodsId(), dto.getBuyQty());
            } catch (Exception unlockEx) {
                log.error("【建单】远程解锁补偿失败，需人工/任务对账 orderNo={}", orderNo, unlockEx);
            }
            throw ex;
        }

        return Result.success("订单创建成功，库存已锁定");
    }

    // ======================== 3. 订单支付 ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> payOrder(Long id) {
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getOrderStatus())) {
            return Result.fail("仅待支付订单可执行支付操作");
        }

        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 支付成功：自动生成待发货单（状态 1=待发货）
        OrderDelivery delivery = new OrderDelivery();
        delivery.setOrderNo(order.getOrderNo());
        delivery.setUserId(order.getUserId());
        delivery.setUserName(order.getUserName());
        delivery.setGoodsId(order.getGoodsId());
        delivery.setGoodsName(order.getGoodsName());
        delivery.setBuyQty(order.getBuyQty());
        delivery.setOrderAmount(order.getOrderAmount());
        delivery.setOrderStatus(1);
        delivery.setLogisticsNo("");
        delivery.setRemark("支付自动生成待发货单");
        delivery.setSort(0);
        delivery.setCreateTime(LocalDateTime.now());
        delivery.setUpdateTime(LocalDateTime.now());
        orderDeliveryMapper.insert(delivery);

        return Result.success("订单支付成功，已自动生成待发货单据");
    }

    // ======================== 4. 取消订单 ========================

    /**
     * 取消订单：与单体一致，先 unlock 再 update 状态；远程失败抛错，不静默吞掉。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelOrder(Long id) {
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        Integer currentStatus = order.getOrderStatus();
        boolean isPending = OrderStatusEnum.PENDING_PAYMENT.getCode().equals(currentStatus);
        boolean isPaid = OrderStatusEnum.PAID.getCode().equals(currentStatus);
        if (!isPending && !isPaid) {
            return Result.fail("仅待支付/已支付订单可取消");
        }

        // 先释放锁定库存（失败抛 BusinessException，本地事务回滚，订单状态不变）
        inventoryStockClient.unlock(order.getGoodsId(), order.getBuyQty());

        order.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        return Result.success("订单已取消，锁定库存已释放");
    }

    // ======================== 5. 确认收货 ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> receiveOrder(Long id) {
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!OrderStatusEnum.SHIPPED.getCode().equals(order.getOrderStatus())) {
            return Result.fail("只有已发货的订单才能确认收货");
        }

        order.setOrderStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setReceiveTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        // 同步发货单状态为 3=已收货
        LambdaUpdateWrapper<OrderDelivery> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderDelivery::getOrderNo, order.getOrderNo())
                .set(OrderDelivery::getOrderStatus, 3);
        orderDeliveryMapper.update(null, updateWrapper);

        return Result.success("确认收货成功，订单已完成");
    }
}
