package com.inventory.modules.order.orderrefund.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.enums.RefundStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;
import com.inventory.modules.order.orderdelivery.mapper.OrderDeliveryMapper;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import com.inventory.modules.order.orderrefund.dto.OrderRefundApplyDTO;
import com.inventory.modules.order.orderrefund.dto.OrderRefundDTO;
import com.inventory.modules.order.orderrefund.entity.OrderRefund;
import com.inventory.modules.order.orderrefund.mapper.OrderRefundMapper;
import com.inventory.modules.order.orderrefund.service.OrderRefundService;
import com.inventory.modules.order.orderrefund.vo.OrderRefundVO;
import com.inventory.order.client.InventoryStockClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单退款/退货 Service 实现（微服务改造版）。
 * <p>
 * 相对单体：删除 StockService；审核通过时通过 {@link InventoryStockClient#increase} 远程回库。
 * </p>
 * <p>
 * <b>事务风险：</b> increase 成功后若本地更新失败，库存已加但退款单仍待审核。
 * 本阶段接受，后续可接补偿/对账。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderRefundServiceImpl extends ServiceImpl<OrderRefundMapper, OrderRefund>
        implements OrderRefundService {

    private final OrderRefundMapper orderRefundMapper;
    private final OrderDeliveryMapper orderDeliveryMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final InventoryStockClient inventoryStockClient;

    @Override
    public Result<?> pageRefundList(Long pageNum, Long pageSize, String orderNo, String refundStatus,
                                    String startTime, String endTime) {
        Page<OrderRefund> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OrderRefund> wrapper = Wrappers.lambdaQuery();

        if (StrUtil.isNotBlank(orderNo)) {
            wrapper.like(OrderRefund::getOrderNo, orderNo);
        }
        if (StrUtil.isNotBlank(refundStatus)) {
            RefundStatusEnum statusEnum = RefundStatusEnum.valueOf(refundStatus);
            wrapper.eq(OrderRefund::getRefundStatus, statusEnum.getCode());
        }
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            wrapper.between(OrderRefund::getApplyTime, startTime, endTime);
        }

        wrapper.orderByDesc(OrderRefund::getCreateTime);
        Page<OrderRefund> refundPage = orderRefundMapper.selectPage(page, wrapper);

        List<OrderRefundVO> voList = new ArrayList<>();
        for (OrderRefund item : refundPage.getRecords()) {
            OrderRefundVO vo = new OrderRefundVO();
            BeanUtil.copyProperties(item, vo);
            RefundStatusEnum statusEnum = RefundStatusEnum.getByCode(item.getRefundStatus());
            vo.setStatusName(statusEnum == null ? "未知状态" : statusEnum.getDesc());
            voList.add(vo);
        }

        Page<OrderRefundVO> voPage = new Page<>(
                refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> applyRefund(OrderRefundApplyDTO dto) {
        OrderInfo order = orderInfoMapper.selectById(dto.getOrderId());
        if (order == null) {
            return Result.fail("原订单不存在");
        }

        Integer status = order.getOrderStatus();
        Integer paid = OrderStatusEnum.PAID.getCode();
        Integer shipped = OrderStatusEnum.SHIPPED.getCode();
        Integer completed = OrderStatusEnum.COMPLETED.getCode();
        if (!status.equals(paid) && !status.equals(shipped) && !status.equals(completed)) {
            return Result.fail("当前订单状态不支持退款");
        }

        OrderRefund refund = new OrderRefund();
        BeanUtil.copyProperties(dto, refund);
        refund.setOrderNo(order.getOrderNo());
        refund.setUserName(order.getUserName());
        refund.setGoodsName(order.getGoodsName());
        if (refund.getRefundAmount() == null) {
            refund.setRefundAmount(order.getOrderAmount());
        }
        refund.setOriginalOrderStatus(order.getOrderStatus());
        refund.setOriginalDeliveryStatus(getCurrentDeliveryStatus(order.getOrderNo()));
        refund.setRefundStatus(RefundStatusEnum.PENDING.getCode());
        refund.setApplyTime(LocalDateTime.now());
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());
        save(refund);

        order.setOrderStatus(OrderStatusEnum.REFUNDING.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        LambdaUpdateWrapper<OrderDelivery> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderDelivery::getOrderNo, order.getOrderNo())
                .set(OrderDelivery::getOrderStatus, 4);
        orderDeliveryMapper.update(null, wrapper);

        return Result.success("退款申请已提交");
    }

    private Integer getCurrentDeliveryStatus(String orderNo) {
        LambdaQueryWrapper<OrderDelivery> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderDelivery::getOrderNo, orderNo);
        OrderDelivery delivery = orderDeliveryMapper.selectOne(qw);
        return delivery == null ? 1 : delivery.getOrderStatus();
    }

    /**
     * 审核通过：远程 increase 回库，再更新订单/退款单状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> approveRefund(Long id, OrderRefundDTO dto) {
        OrderRefund refund = getById(id);
        if (refund == null) {
            return Result.fail("退款单不存在");
        }
        if (!RefundStatusEnum.PENDING.getCode().equals(refund.getRefundStatus())) {
            return Result.fail("仅待审核的退款单可通过");
        }

        OrderInfo order = orderInfoMapper.selectById(refund.getOrderId());
        if (order == null) {
            return Result.fail("关联订单不存在");
        }

        // 远程退货入库（失败抛异常，不改本地状态）
        inventoryStockClient.increase(order.getGoodsId(), order.getBuyQty());

        order.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        refund.setRefundStatus(RefundStatusEnum.APPROVED.getCode());
        if (dto != null && StrUtil.isNotBlank(dto.getRemark())) {
            refund.setAuditRemark(dto.getRemark());
        }
        refund.setUpdateTime(LocalDateTime.now());
        updateById(refund);

        return Result.success("退款审核通过，库存已恢复，订单状态已更新为【退款完成】");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rejectRefund(Long id, OrderRefundDTO dto) {
        OrderRefund refund = getById(id);
        if (refund == null) {
            return Result.fail("退款单不存在");
        }
        if (!RefundStatusEnum.PENDING.getCode().equals(refund.getRefundStatus())) {
            return Result.fail("仅待审核的退款单可拒绝");
        }

        refund.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
        if (dto != null && StrUtil.isNotBlank(dto.getRemark())) {
            refund.setAuditRemark(dto.getRemark());
        }
        refund.setUpdateTime(LocalDateTime.now());
        updateById(refund);

        String orderNo = refund.getOrderNo();
        LambdaUpdateWrapper<OrderInfo> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(OrderInfo::getOrderNo, orderNo)
                .set(OrderInfo::getOrderStatus, refund.getOriginalOrderStatus())
                .set(OrderInfo::getUpdateTime, LocalDateTime.now());
        orderInfoMapper.update(null, orderUpdate);

        LambdaUpdateWrapper<OrderDelivery> deliveryUpdate = new LambdaUpdateWrapper<>();
        deliveryUpdate.eq(OrderDelivery::getOrderNo, orderNo)
                .set(OrderDelivery::getOrderStatus, refund.getOriginalDeliveryStatus());
        orderDeliveryMapper.update(null, deliveryUpdate);

        return Result.success("已拒绝退款，订单状态已还原");
    }
}
