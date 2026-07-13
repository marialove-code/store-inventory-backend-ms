package com.inventory.modules.order.orderdelivery.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;
import com.inventory.modules.order.orderdelivery.mapper.OrderDeliveryMapper;
import com.inventory.modules.order.orderdelivery.service.OrderDeliveryService;
import com.inventory.modules.order.orderdelivery.vo.OrderDeliveryVO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import com.inventory.order.client.InventoryStockClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单发货 Service 实现（微服务改造版）。
 * <p>
 * 相对单体：删除 GoodsProductMapper、InventoryStockMapper、StockService；
 * 确认发货时通过 {@link InventoryStockClient#decreaseFlow} 远程扣减库存。
 * </p>
 * <p>
 * <b>事务风险：</b> decreaseFlow 成功后若本地更新失败，本地事务回滚但库存已扣减，
 * 会出现「库存已出、发货单仍待发货」。本阶段接受，后续可用对账/补偿任务处理。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderDeliveryServiceImpl extends ServiceImpl<OrderDeliveryMapper, OrderDelivery>
        implements OrderDeliveryService {

    private final OrderDeliveryMapper orderDeliveryMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final InventoryStockClient inventoryStockClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result<?> pageDeliveryList(String orderNo, String startTime, String endTime,
                                      Long pageNum, Long pageSize) {
        Long finalPageNum = pageNum == null ? 1L : pageNum;
        Long finalPageSize = pageSize == null ? 10L : pageSize;

        Page<OrderDelivery> page = new Page<>(finalPageNum, finalPageSize);
        LambdaQueryWrapper<OrderDelivery> wrapper = Wrappers.lambdaQuery();

        if (StrUtil.isNotBlank(orderNo)) {
            wrapper.like(OrderDelivery::getOrderNo, orderNo.trim());
        }
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime.trim(), DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endTime.trim(), DATE_TIME_FORMATTER);
                wrapper.between(OrderDelivery::getCreateTime, start, end);
            } catch (Exception e) {
                return Result.fail("时间格式错误，请使用：yyyy-MM-dd HH:mm:ss");
            }
        }

        wrapper.orderByDesc(OrderDelivery::getCreateTime);
        Page<OrderDelivery> resultPage = orderDeliveryMapper.selectPage(page, wrapper);

        List<OrderDeliveryVO> voList = new ArrayList<>();
        for (OrderDelivery item : resultPage.getRecords()) {
            OrderDeliveryVO vo = new OrderDeliveryVO();
            BeanUtil.copyProperties(item, vo);
            OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(item.getOrderStatus());
            vo.setStatusName(statusEnum == null ? "未知状态" : statusEnum.getDesc());
            voList.add(vo);
        }

        Page<OrderDeliveryVO> voPage = new Page<>(
                resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 确认发货：先远程 decreaseFlow，再更新发货单与主订单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> confirmDelivery(Long id, OrderDeliveryDTO dto) {
        OrderDelivery deliveryOrder = getById(id);
        if (deliveryOrder == null) {
            return Result.fail("发货单不存在");
        }
        if (!OrderStatusEnum.PAID.getCode().equals(deliveryOrder.getOrderStatus())) {
            return Result.fail("仅【待发货】订单可以发货");
        }

        // 远程发货扣减（失败抛异常，本地事务不改状态）
        inventoryStockClient.decreaseFlow(
                deliveryOrder.getGoodsId(),
                deliveryOrder.getBuyQty(),
                dto.getLogisticsNo()
        );

        deliveryOrder.setLogisticsNo(dto.getLogisticsNo());
        deliveryOrder.setRemark(dto.getRemark());
        deliveryOrder.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        deliveryOrder.setUpdateTime(LocalDateTime.now());
        updateById(deliveryOrder);

        OrderInfo orderInfo = orderInfoMapper.selectOne(
                Wrappers.lambdaQuery(OrderInfo.class)
                        .eq(OrderInfo::getOrderNo, deliveryOrder.getOrderNo())
        );
        if (orderInfo != null) {
            orderInfo.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
            orderInfo.setLogisticsNo(dto.getLogisticsNo());
            orderInfo.setUpdateTime(LocalDateTime.now());
            orderInfoMapper.updateById(orderInfo);
        }

        return Result.success("发货成功，订单已同步更新为【已发货】");
    }
}
