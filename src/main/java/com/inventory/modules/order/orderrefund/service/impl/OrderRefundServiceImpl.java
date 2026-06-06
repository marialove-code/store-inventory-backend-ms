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
import com.inventory.modules.invertory.stock.service.StockService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单退款/退货 Service 实现类
 *
 * 功能说明：
 * 1. 退款/退货申请、审核、列表查询
 * 2. 退款审核通过 → 自动将商品退回库存
 * 3. 状态统一使用枚举管理，保证业务一致性
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class OrderRefundServiceImpl extends ServiceImpl<OrderRefundMapper, OrderRefund> implements OrderRefundService {

    /**
     * 退款单 Mapper
     */
    private final OrderRefundMapper orderRefundMapper;

    /**
     * 发货管理
     */
    private final OrderDeliveryMapper orderDeliveryMapper;

    /**
     * 订单 Mapper（查询原订单信息）
     */
    private final OrderInfoMapper orderInfoMapper;

    /**
     * 库存核心服务（退货时增加库存）
     */
    private final StockService stockService;

    // ======================== 1. 分页查询退款/退货列表 ========================
    /**
     * 退款单分页多条件查询
     * 支持：订单号、退款状态、申请时间范围
     * 返回：带状态中文名称的 VO 分页数据
     */
    @Override
    public Result<?> pageRefundList(Long pageNum, Long pageSize, String orderNo, String refundStatus,
                                    String startTime, String endTime) {
        // 1. 初始化分页对象
        Page<OrderRefund> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件
        LambdaQueryWrapper<OrderRefund> wrapper = Wrappers.lambdaQuery();

        // 3. 订单号模糊匹配
        if (StrUtil.isNotBlank(orderNo)) {
            wrapper.like(OrderRefund::getOrderNo, orderNo);
        }

        // 4. 退款状态精确匹配
        if (StrUtil.isNotBlank(refundStatus)) {
            RefundStatusEnum statusEnum = RefundStatusEnum.valueOf(refundStatus);
            wrapper.eq(OrderRefund::getRefundStatus, statusEnum.getCode());
        }

        // 5. 申请时间范围查询
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            wrapper.between(OrderRefund::getApplyTime, startTime, endTime);
        }

        // 6. 排序：创建时间倒序
        wrapper.orderByDesc(OrderRefund::getCreateTime);

        // 7. 执行分页查询
        Page<OrderRefund> refundPage = orderRefundMapper.selectPage(page, wrapper);

        // ===================== 8. 实体转VO（替换Stream为for循环） =====================
        List<OrderRefund> records = refundPage.getRecords();
        List<OrderRefundVO> voList = new ArrayList<>();

        for (OrderRefund item : records) {
            OrderRefundVO vo = new OrderRefundVO();
            BeanUtil.copyProperties(item, vo);

            // 9. 设置状态中文名称
            RefundStatusEnum statusEnum = RefundStatusEnum.getByCode(item.getRefundStatus());
            String statusName;
            if (statusEnum == null) {
                statusName = "未知状态";
            } else {
                statusName = statusEnum.getDesc();
            }
            vo.setStatusName(statusName);

            voList.add(vo);
        }

        // 10. 封装VO分页对象
        Page<OrderRefundVO> voPage = new Page<>(
                refundPage.getCurrent(),
                refundPage.getSize(),
                refundPage.getTotal()
        );
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    // ======================== 2. 用户提交退款/退货申请 ========================
    /**
     * 提交退款申请
     * 逻辑：
     * 1. 校验原订单是否存在
     * 2. 自动填充订单信息
     * 3. 生成退款单，状态为待审核
     */
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

        // ====================== 关键：保存原始状态 ======================
        refund.setOriginalOrderStatus(order.getOrderStatus()); // 订单原始状态
        refund.setOriginalDeliveryStatus(getCurrentDeliveryStatus(order.getOrderNo())); // 发货单原始状态

        refund.setRefundStatus(RefundStatusEnum.PENDING.getCode());
        refund.setApplyTime(LocalDateTime.now());
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());

        save(refund);

        // 更新订单为退款中
        order.setOrderStatus(OrderStatusEnum.REFUNDING.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        // 更新发货单为退款中
        LambdaUpdateWrapper<OrderDelivery> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderDelivery::getOrderNo, order.getOrderNo())
                .set(OrderDelivery::getOrderStatus, 4);
        orderDeliveryMapper.update(null, wrapper);

        return Result.success("退款申请已提交");
    }

    // 工具方法：获取当前发货单状态
    private Integer getCurrentDeliveryStatus(String orderNo) {
        LambdaQueryWrapper<OrderDelivery> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderDelivery::getOrderNo, orderNo);
        OrderDelivery delivery = orderDeliveryMapper.selectOne(qw);
        return delivery == null ? 1 : delivery.getOrderStatus();
    }

    // ======================== 3. 管理员审核退款：通过 ========================
    /**
     * 审核退款通过
     * 逻辑：
     * 1. 校验退款单状态
     * 2. 校验原订单
     * 3. 退货入库 → 增加库存
     * 4. 更新状态为审核通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> approveRefund(Long id, OrderRefundDTO dto) {
        // 1. 查询退款单
        OrderRefund refund = getById(id);
        if (refund == null) {
            return Result.fail("退款单不存在");
        }

        // 2. 仅待审核可通过
        Integer pendingCode = RefundStatusEnum.PENDING.getCode();
        Integer currentStatus = refund.getRefundStatus();

        if (!pendingCode.equals(currentStatus)) {
            return Result.fail("仅待审核的退款单可通过");
        }

        // 3. 查询原订单（获取商品ID、购买数量）
        OrderInfo order = orderInfoMapper.selectById(refund.getOrderId());
        if (order == null) {
            return Result.fail("关联订单不存在");
        }

        // ===================== 4. 核心：退货入库，增加可用库存 =====================
        stockService.increaseStock(order.getGoodsId(), order.getBuyQty());

        // 5. 更新为审核通过状态
        refund.setRefundStatus(RefundStatusEnum.APPROVED.getCode());

        // 6. 填写审核备注
        if (dto != null) {
            String remark = dto.getRemark();
            if (StrUtil.isNotBlank(remark)) {
                refund.setAuditRemark(remark);
            }
        }

        // 7. 更新时间
        refund.setUpdateTime(LocalDateTime.now());
        updateById(refund);

        return Result.success("审核通过，商品已退回库存");
    }

    // ======================== 4. 管理员审核退款：拒绝 ========================
    /**
     * 审核退款拒绝
     * 逻辑：仅修改状态，不操作库存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rejectRefund(Long id, OrderRefundDTO dto) {
        // 1. 查询退款单
        OrderRefund refund = getById(id);
        if (refund == null) {
            return Result.fail("退款单不存在");
        }

        // 2. 仅待审核可拒绝
        Integer pendingCode = RefundStatusEnum.PENDING.getCode();
        if (!pendingCode.equals(refund.getRefundStatus())) {
            return Result.fail("仅待审核的退款单可拒绝");
        }

        // 3. 拒绝退款
        refund.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
        if (dto != null && StrUtil.isNotBlank(dto.getRemark())) {
            refund.setAuditRemark(dto.getRemark());
        }
        refund.setUpdateTime(LocalDateTime.now());
        updateById(refund);

        // ====================== 关键：还原订单状态 ======================
        String orderNo = refund.getOrderNo();
        Integer originalOrderStatus = refund.getOriginalOrderStatus();
        Integer originalDeliveryStatus = refund.getOriginalDeliveryStatus();

        // 还原订单
        LambdaUpdateWrapper<OrderInfo> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(OrderInfo::getOrderNo, orderNo)
                .set(OrderInfo::getOrderStatus, originalOrderStatus)
                .set(OrderInfo::getUpdateTime, LocalDateTime.now());
        orderInfoMapper.update(null, orderUpdate);

        // 还原发货单
        LambdaUpdateWrapper<OrderDelivery> deliveryUpdate = new LambdaUpdateWrapper<>();
        deliveryUpdate.eq(OrderDelivery::getOrderNo, orderNo)
                .set(OrderDelivery::getOrderStatus, originalDeliveryStatus);
        orderDeliveryMapper.update(null, deliveryUpdate);

        return Result.success("已拒绝退款，订单状态已还原");
    }
}