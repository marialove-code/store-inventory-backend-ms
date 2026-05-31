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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单信息 服务实现类
 *
 * @author 95349
 * @date 2026-05-31
 */
@Service
@RequiredArgsConstructor
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private final OrderInfoMapper orderInfoMapper;

    /**
     * 订单分页列表查询
     *
     * @param orderNo     订单号
     * @param goodsName   商品名称
     * @param orderStatus 订单状态
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @return 分页结果（包含状态名称 statusName）
     */

    @Override
    public Result<?> pageOrderList(String orderNo, String goodsName, String orderStatus,
                                   String startTime, String endTime, Long pageNum, Long pageSize) {
        // 构建分页对象
        Page<OrderInfo> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<OrderInfo> wrapper = Wrappers.lambdaQuery();

        // 订单号精确匹配
        if (StrUtil.isNotBlank(orderNo)) {
            wrapper.eq(OrderInfo::getOrderNo, orderNo);
        }

        // 商品名称模糊查询
        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(OrderInfo::getGoodsName, goodsName);
        }

        // 订单状态精确匹配
        if (orderStatus != null) {
            wrapper.eq(OrderInfo::getOrderStatus, orderStatus);
        }

        // 创建时间范围查询
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            wrapper.between(OrderInfo::getCreateTime, startTime, endTime);
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(OrderInfo::getCreateTime);

        // 执行分页查询
        Page<OrderInfo> orderPage = orderInfoMapper.selectPage(page, wrapper);

        // 转换为VO分页对象，并设置状态名称
        Page<OrderListVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderListVO> records = orderPage.getRecords().stream()
                .map(item -> {
                    OrderListVO vo = new OrderListVO();
                    BeanUtil.copyProperties(item, vo);

                    // 根据状态code获取状态名称
                    OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(item.getOrderStatus());
                    vo.setStatusName(statusEnum.getDesc());

                    return vo;
                }).collect(Collectors.toList());

        voPage.setRecords(records);
        return Result.success(voPage);
    }

    /**
     * 新建订单
     *
     * @param dto 订单参数
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createOrder(OrderInfoDTO dto) {
        OrderInfo order = new OrderInfo();
        BeanUtil.copyProperties(dto, order);

        // 新建订单默认状态：待支付
        order.setOrderStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        BigDecimal amount = dto.getSalePrice().multiply(BigDecimal.valueOf(dto.getBuyQty()));
        order.setCostPrice(amount);
        String generate = OrderNoGenerator.generate(OrderPrefix.ORDER);
        order.setOrderNo(generate);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        save(order);
        return Result.success("创建订单成功");
    }

    /**
     * 订单支付
     *
     * @param id 订单ID
     * @return 支付结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> payOrder(Long id) {
        // 根据ID查询订单
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 只有待支付订单才能支付
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(order.getOrderStatus())) {
            return Result.fail("仅待支付订单可支付");
        }

        // 更新为已支付状态，并记录支付时间
        order.setOrderStatus(OrderStatusEnum.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        updateById(order);
        return Result.success("支付成功");
    }

    /**
     * 取消订单
     *
     * @param id 订单ID
     * @return 取消结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelOrder(Long id) {
        // 根据ID查询订单
        OrderInfo order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        Integer status = order.getOrderStatus();
        // 只有待支付、已支付订单可以取消
        if (!OrderStatusEnum.PENDING_PAYMENT.getCode().equals(status)
                && !OrderStatusEnum.PAID.getCode().equals(status)) {
            return Result.fail("仅待支付、已支付订单可取消");
        }

        // 更新为已取消状态
        order.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        order.setUpdateTime(LocalDateTime.now());

        updateById(order);
        return Result.success("取消成功");
    }

}