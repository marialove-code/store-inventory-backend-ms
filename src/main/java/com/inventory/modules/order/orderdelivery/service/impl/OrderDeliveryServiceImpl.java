package com.inventory.modules.order.orderdelivery.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;
import com.inventory.modules.order.orderdelivery.service.OrderDeliveryService;
import com.inventory.modules.order.orderdelivery.mapper.OrderDeliveryMapper;
import com.inventory.modules.order.orderdelivery.vo.OrderDeliveryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单发货 Service 实现类
 *
 * 功能说明：
 * 1. 待发货订单分页查询（只展示已支付订单）
 * 2. 确认发货（扣减库存 + 更新订单状态）
 * 3. 订单状态使用枚举管理，保证状态统一
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class OrderDeliveryServiceImpl extends ServiceImpl<OrderDeliveryMapper, OrderDelivery>
        implements OrderDeliveryService {

    /**
     * 订单发货 Mapper
     */
    private final OrderDeliveryMapper orderDeliveryMapper;

    /**
     * 库存核心服务（发货时扣减库存）
     */
    private final StockService stockService;

    /**
     * 时间格式化模板：yyyy-MM-dd HH:mm:ss
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ======================== 1. 分页查询待发货订单 ========================
    /**
     * 分页查询待发货列表
     * 只查询【已支付】状态的订单
     * 支持：订单号、创建时间范围筛选
     */
    @Override
    public Result<?> pageDeliveryList(String orderNo, String startTime, String endTime, Long pageNum, Long pageSize) {
        // ===================== 1. 分页参数处理（空值赋默认值） =====================
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
        Page<OrderDelivery> page = new Page<>(finalPageNum, finalPageSize);

        // ===================== 3. 构建查询条件 =====================
        LambdaQueryWrapper<OrderDelivery> wrapper = Wrappers.lambdaQuery();

        // 4. 只查询已支付订单（待发货）
        wrapper.eq(OrderDelivery::getOrderStatus, OrderStatusEnum.PAID.getCode());

        // 5. 订单号模糊匹配
        if (StrUtil.isNotBlank(orderNo)) {
            String trimOrderNo = orderNo.trim();
            wrapper.like(OrderDelivery::getOrderNo, trimOrderNo);
        }

        // 6. 时间范围查询（格式校验）
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            try {
                String trimStart = startTime.trim();
                String trimEnd = endTime.trim();
                LocalDateTime start = LocalDateTime.parse(trimStart, DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(trimEnd, DATE_TIME_FORMATTER);
                wrapper.between(OrderDelivery::getCreateTime, start, end);
            } catch (Exception e) {
                return Result.fail("时间格式错误，请使用：yyyy-MM-dd HH:mm:ss");
            }
        }

        // 7. 排序：创建时间倒序（最新订单优先）
        wrapper.orderByDesc(OrderDelivery::getCreateTime);

        // 8. 执行分页查询
        Page<OrderDelivery> resultPage = orderDeliveryMapper.selectPage(page, wrapper);

        // ===================== 9. 实体列表转换为VO列表（替换Stream为for循环） =====================
        List<OrderDelivery> records = resultPage.getRecords();
        List<OrderDeliveryVO> voList = new ArrayList<>();

        for (OrderDelivery item : records) {
            OrderDeliveryVO vo = new OrderDeliveryVO();
            // 属性拷贝（保持不变）
            BeanUtil.copyProperties(item, vo);

            // 10. 状态枚举转换：设置状态中文名称
            OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(item.getOrderStatus());
            String statusName;
            if (statusEnum == null) {
                statusName = "未知状态";
            } else {
                statusName = statusEnum.getDesc();
            }
            vo.setStatusName(statusName);

            voList.add(vo);
        }

        // ===================== 11. 封装VO分页对象 =====================
        Page<OrderDeliveryVO> voPage = new Page<>(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal()
        );
        voPage.setRecords(voList);

        // 12. 返回成功结果
        return Result.success(voPage);
    }

    // ======================== 2. 确认发货（核心业务） ========================
    /**
     * 订单确认发货
     * 逻辑：
     * 1. 校验订单是否存在
     * 2. 仅允许【已支付】订单发货
     * 3. 调用库存服务扣减真实库存
     * 4. 更新订单为【已发货】状态
     * 5. 记录物流单号、备注、更新时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> confirmDelivery(Long id, OrderDeliveryDTO dto) {
        // 1. 根据ID查询订单
        OrderDelivery order = getById(id);

        // 2. 订单不存在判断
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 3. 仅已支付订单可以发货
        Integer paidCode = OrderStatusEnum.PAID.getCode();
        Integer orderStatus = order.getOrderStatus();
        if (!paidCode.equals(orderStatus)) {
            return Result.fail("仅【已支付】订单可以发货");
        }

        // ===================== 4. 扣减真实库存（最终业务闭环） =====================
        stockService.decreaseStock(order.getGoodsId(), order.getBuyQty());

        // 5. 设置发货信息
        order.setLogisticsNo(dto.getLogisticsNo());
        order.setRemark(dto.getRemark());

        // 6. 更新订单状态为【已发货】
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());

        // 7. 更新时间
        order.setUpdateTime(LocalDateTime.now());

        // 8. 执行更新
        updateById(order);

        return Result.success("发货成功，库存已扣减");
    }

}