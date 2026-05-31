package com.inventory.modules.order.orderrefund.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.enums.RefundStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderrefund.dto.OrderRefundApplyDTO;
import com.inventory.modules.order.orderrefund.dto.OrderRefundDTO;
import com.inventory.modules.order.orderrefund.entity.OrderRefund;
import com.inventory.modules.order.orderrefund.service.OrderRefundService;
import com.inventory.modules.order.orderrefund.mapper.OrderRefundMapper;
import com.inventory.modules.order.orderrefund.vo.OrderRefundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【order_refund】的数据库操作Service实现
* @createDate 2026-05-29 19:04:18
*/
@Service
@RequiredArgsConstructor
public class OrderRefundServiceImpl extends ServiceImpl<OrderRefundMapper, OrderRefund>
    implements OrderRefundService{

    private final OrderRefundMapper orderRefundMapper;

    /**
     * 退款订单分页列表
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param orderNo      订单号（模糊查询）
     * @param refundStatus 退款状态
     * @param startTime    申请时间起
     * @param endTime      申请时间止
     * @return 分页结果
     */
    @Override
    public Result<?> pageRefundList(Long pageNum, Long pageSize, String orderNo, String refundStatus, String startTime, String endTime) {
        // 构建分页对象
        Page<OrderRefund> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<OrderRefund> wrapper = Wrappers.lambdaQuery();

        // 订单号模糊查询
        if (StrUtil.isNotBlank(orderNo)) {
            wrapper.like(OrderRefund::getOrderNo, orderNo);
        }

        // 退款状态精确匹配
        if (StrUtil.isNotBlank(refundStatus)) {
            RefundStatusEnum statusEnum = RefundStatusEnum.valueOf(refundStatus);
            wrapper.eq(OrderRefund::getRefundStatus, statusEnum.getCode());
        }

        // 申请时间范围查询
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            wrapper.between(OrderRefund::getApplyTime, startTime, endTime);
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(OrderRefund::getCreateTime);

        // 执行分页查询
        Page<OrderRefund> refundPage = orderRefundMapper.selectPage(page, wrapper);

        // 转换为VO分页对象，并设置状态名称
        Page<OrderRefundVO> voPage = new Page<>(refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());
        List<OrderRefundVO> records = refundPage.getRecords().stream()
                .map(item -> {
                    OrderRefundVO vo = new OrderRefundVO();
                    BeanUtil.copyProperties(item, vo);

                    // 根据状态code获取状态名称
                    RefundStatusEnum statusEnum = RefundStatusEnum.getByCode(item.getRefundStatus());
                    vo.setStatusName(statusEnum == null ? "未知状态" : statusEnum.getDesc());

                    return vo;
                }).collect(Collectors.toList());

        voPage.setRecords(records);
        return Result.success(voPage);
    }

    /**
     * 发起退款申请
     * 业务规则：由订单列表发起，关联订单信息
     *
     * @param dto 退款申请参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> applyRefund(OrderRefundApplyDTO dto) {
        // 此处应根据 orderId 查询原订单信息，获取订单号、用户名称、商品名称、订单金额等信息
        // 为简化示例，此处直接赋值，实际业务中需补充查询逻辑
        OrderRefund refund = new OrderRefund();
        BeanUtil.copyProperties(dto, refund);

        // 初始化退款状态为待审核
        refund.setRefundStatus(RefundStatusEnum.PENDING.getCode());
        refund.setApplyTime(LocalDateTime.now());
        refund.setCreateTime(LocalDateTime.now());
        refund.setUpdateTime(LocalDateTime.now());

        save(refund);
        return Result.success("退款申请已提交");
    }

    /**
     * 通过退款申请
     * 业务规则：
     * 1. 退款单必须存在
     * 2. 状态必须为待审核
     * 3. 更新为已通过，并记录审核备注
     *
     * @param id  退款单ID
     * @param dto 审核参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> approveRefund(Long id, OrderRefundDTO dto) {
        OrderRefund refund = getById(id);
        if (refund == null) {
            return Result.fail("退款单不存在");
        }

        // 校验状态：仅待审核可通过
        if (!RefundStatusEnum.PENDING.getCode().equals(refund.getRefundStatus())) {
            return Result.fail("仅待审核的退款单可通过");
        }

        // 更新退款状态和审核信息
        refund.setRefundStatus(RefundStatusEnum.APPROVED.getCode());
        if (dto != null && StrUtil.isNotBlank(dto.getRemark())) {
            refund.setAuditRemark(dto.getRemark());
        }
        refund.setUpdateTime(LocalDateTime.now());

        updateById(refund);
        return Result.success("退款已通过，库存将自动回滚");
    }

    /**
     * 拒绝退款申请
     * 业务规则：
     * 1. 退款单必须存在
     * 2. 状态必须为待审核
     * 3. 更新为已拒绝，并记录审核备注
     *
     * @param id  退款单ID
     * @param dto 审核参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rejectRefund(Long id, OrderRefundDTO dto) {
        OrderRefund refund = getById(id);
        if (refund == null) {
            return Result.fail("退款单不存在");
        }

        // 校验状态：仅待审核可拒绝
        if (!RefundStatusEnum.PENDING.getCode().equals(refund.getRefundStatus())) {
            return Result.fail("仅待审核的退款单可拒绝");
        }

        // 更新退款状态和审核信息
        refund.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
        if (dto != null && StrUtil.isNotBlank(dto.getRemark())) {
            refund.setAuditRemark(dto.getRemark());
        }
        refund.setUpdateTime(LocalDateTime.now());

        updateById(refund);
        return Result.success("退款已拒绝");
    }

}




