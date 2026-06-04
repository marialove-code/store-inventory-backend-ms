package com.inventory.modules.order.orderinfo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import com.inventory.modules.order.orderinfo.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务 Service 实现类
 *
 * 功能说明：
 * 1. 订单全生命周期管理：创建、支付、取消、发货
 * 2. 库存闭环：创建锁定 → 取消释放 → 发货扣减
 * 3. 所有库存操作统一调用 StockService，保证事务与一致性
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    /**
     * 订单 Mapper
     */
    private final OrderInfoMapper orderInfoMapper;

    /**
     * 库存核心服务
     */
    private final StockService stockService;

    /**
     * 库存实时表 Mapper（创建订单前校验库存）
     */
    private final InventoryStockMapper inventoryStockMapper;

    /**
     * 时间格式化：yyyy-MM-dd HH:mm:ss
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ======================== 1. 订单分页列表查询 ========================
    /**
     * 订单分页多条件查询
     * 支持：订单号、商品名称、订单状态、时间范围
     * 返回：带状态中文名称的 VO 分页数据
     */
    @Override
    public Result<?> pageOrderList(String orderNo, String goodsName, String orderStatus,
                                   String startTime, String endTime, Long pageNum, Long pageSize) {
        // ===================== 1. 分页参数默认值处理 =====================
        Long finalPageNum;
        if (pageNum == null) {
            finalPageNum = 1L;
        } else {
            finalPageNum = pageNum;
        }

        Long finalPageSize;
        if (pageSize == null) {
            finalPageSize = 10L;
        } else {
            finalPageSize = pageSize;
        }

        // 2. 初始化分页对象
        Page<OrderInfo> page = new Page<>(finalPageNum, finalPageSize);

        // ===================== 3. 构建查询条件 =====================
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery();

        // 4. 订单号精确匹配
        if (StrUtil.isNotBlank(orderNo)) {
            String trimOrderNo = orderNo.trim();
            queryWrapper.eq(OrderInfo::getOrderNo, trimOrderNo);
        }

        // 5. 商品名称模糊匹配
        if (StrUtil.isNotBlank(goodsName)) {
            String trimGoodsName = goodsName.trim();
            queryWrapper.like(OrderInfo::getGoodsName, trimGoodsName);
        }

        // 6. 订单状态精确匹配
        if (StrUtil.isNotBlank(orderStatus)) {
            try {
                String trimStatus = orderStatus.trim();
                Integer statusCode = Integer.parseInt(trimStatus);
                queryWrapper.eq(OrderInfo::getOrderStatus, statusCode);
            } catch (NumberFormatException e) {
                return Result.fail("订单状态格式错误");
            }
        }

        // 7. 时间范围查询
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            try {
                String trimStart = startTime.trim();
                String trimEnd = endTime.trim();
                LocalDateTime start = LocalDateTime.parse(trimStart, DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(trimEnd, DATE_TIME_FORMATTER);
                queryWrapper.between(OrderInfo::getCreateTime, start, end);
            } catch (Exception e) {
                return Result.fail("时间格式错误：yyyy-MM-dd HH:mm:ss");
            }
        }

        // 8. 排序：创建时间倒序
        queryWrapper.orderByDesc(OrderInfo::getCreateTime);

        // 9. 执行分页查询
        Page<OrderInfo> orderPage = orderInfoMapper.selectPage(page, queryWrapper);

        // ===================== 10. 实体转VO（替换Stream为for循环） =====================
        List<OrderInfo> records = orderPage.getRecords();
        List<OrderListVO> voList = new ArrayList<>();

        for (OrderInfo order : records) {
            OrderListVO vo = new OrderListVO();
            BeanUtil.copyProperties(order, vo);

            // 11. 设置状态中文名称
            OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(order.getOrderStatus());
            String statusName;
            if (statusEnum == null) {
                statusName = "未知状态";
            } else {
                statusName = statusEnum.getDesc();
            }
            vo.setStatusName(statusName);

            voList.add(vo);
        }

        // 12. 封装VO分页对象
        Page<OrderListVO> voPage = new Page<>(
                orderPage.getCurrent(),
                orderPage.getSize(),
                orderPage.getTotal()
        );
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    // ======================== 2. 创建订单（核心） ========================
    /**
     * 创建订单
     * 逻辑：
     * 1. 校验库存是否充足
     * 2. 生成订单与订单号
     * 3. 锁定库存（预占）
     * 4. 事务保证一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createOrder(OrderInfoDTO dto) {
        // ===================== 1. 库存充足性校验 =====================
        LambdaQueryWrapper<InventoryStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.eq(InventoryStock::getGoodsId, dto.getGoodsId());
        InventoryStock stock = inventoryStockMapper.selectOne(stockWrapper);

        if (stock == null) {
            return Result.fail("商品库存不存在");
        }

        // 可用库存 = 总库存 - 锁定库存
        int totalStock = stock.getStock();
        int lockStock = stock.getLockStock();
        int usableStock = totalStock - lockStock;

        if (usableStock < dto.getBuyQty()) {
            return Result.fail("库存不足，当前可用库存：" + usableStock);
        }

        // ===================== 2. 构建订单实体 =====================
        OrderInfo order = new OrderInfo();
        BeanUtil.copyProperties(dto, order);

        // 3. 生成唯一订单号
        String orderNo = OrderNoGenerator.generate(OrderPrefix.ORDER);
        order.setOrderNo(orderNo);

        // 4. 计算订单总金额
        BigDecimal salePrice = dto.getSalePrice();
        Integer buyQty = dto.getBuyQty();
        BigDecimal orderAmount = salePrice.multiply(BigDecimal.valueOf(buyQty));
        order.setOrderAmount(orderAmount);

        // 5. 订单默认状态：待支付
        order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 6. 保存订单
        save(order);

        // ===================== 7. 锁定库存（预占） =====================
        stockService.lockStock(dto.getGoodsId(), dto.getBuyQty());

        return Result.success("订单创建成功，库存已锁定");
    }

    // ======================== 3. 订单支付 ========================
    /**
     * 订单支付
     * 仅允许：待支付 → 已支付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> payOrder(Long id) {
        // 1. 查询订单
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 2. 仅待支付可支付
        Integer pendingCode = OrderStatusEnum.PENDING_PAYMENT.getCode();
        Integer currentStatus = order.getOrderStatus();

        if (!pendingCode.equals(currentStatus)) {
            return Result.fail("仅待支付订单可执行支付操作");
        }

        // 3. 更新为已支付状态
        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        return Result.success("订单支付成功");
    }

    // ======================== 4. 取消订单 ========================
    /**
     * 取消订单
     * 逻辑：
     * 1. 仅待支付/已支付可取消
     * 2. 取消后释放锁定库存
     * 3. 更新订单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelOrder(Long id) {
        // 1. 查询订单
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 2. 状态校验：仅允许 待支付 / 已支付 取消
        Integer currentStatus = order.getOrderStatus();
        Integer pendingCode = OrderStatusEnum.PENDING_PAYMENT.getCode();
        Integer paidCode = OrderStatusEnum.PAID.getCode();

        boolean isPending = pendingCode.equals(currentStatus);
        boolean isPaid = paidCode.equals(currentStatus);

        if (!isPending && !isPaid) {
            return Result.fail("仅待支付/已支付订单可取消");
        }

        // ===================== 3. 释放锁定库存 =====================
        stockService.unlockStock(order.getGoodsId(), order.getBuyQty());

        // 4. 更新为已取消状态
        order.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        return Result.success("订单已取消，锁定库存已释放");
    }

    // ======================== 5. 订单发货 ========================
    /**
     * 订单发货
     * 逻辑：
     * 1. 仅已支付可发货
     * 2. 发货扣减真实库存
     * 3. 更新状态为已发货
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deliverOrder(Long id, String logisticsNo) {
        // 1. 查询订单
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 2. 仅已支付可发货
        Integer paidCode = OrderStatusEnum.PAID.getCode();
        Integer currentStatus = order.getOrderStatus();

        if (!paidCode.equals(currentStatus)) {
            return Result.fail("仅已支付订单可发货");
        }

        // ===================== 3. 扣减真实库存 =====================
        stockService.decreaseStock(order.getGoodsId(), order.getBuyQty());

        // 4. 更新发货信息
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        order.setLogisticsNo(logisticsNo);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);

        return Result.success("订单发货成功，库存已正式扣减");
    }
}