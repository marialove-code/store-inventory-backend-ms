package com.inventory.modules.order.orderdelivery.impl;

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
import com.inventory.modules.order.orderdelivery.service.OrderDeliveryService;
import com.inventory.modules.order.orderdelivery.mapper.OrderDeliveryMapper;
import com.inventory.modules.order.orderdelivery.vo.OrderDeliveryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【order_delivery】的数据库操作Service实现
* @createDate 2026-05-29 19:04:18
*/
@Service
@RequiredArgsConstructor
public class OrderDeliveryServiceImpl extends ServiceImpl<OrderDeliveryMapper, OrderDelivery>
    implements OrderDeliveryService{

    private final OrderDeliveryMapper orderDeliveryMapper;

    /**
     * 待发货订单分页列表
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param orderNo    订单号（模糊查询）
     * @param startTime  创建时间-开始
     * @param endTime    创建时间-结束
     * @return 分页数据（OrderDeliveryVO）
     */
    @Override
    public Result<?> pageDeliveryList(Long pageNum, Long pageSize, String orderNo, String startTime, String endTime) {
        // 1. 构建分页对象
        Page<OrderDelivery> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件：只查询 已支付 订单
        LambdaQueryWrapper<OrderDelivery> wrapper = Wrappers.lambdaQuery();
//

        // 3. 动态条件
        // 订单号模糊匹配
        if (StrUtil.isNotBlank(orderNo)) {
            wrapper.like(OrderDelivery::getOrderNo, orderNo);
        }
        // 创建时间范围
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            wrapper.between(OrderDelivery::getCreateTime, startTime, endTime);
        }

        // 4. 排序：创建时间倒序
        wrapper.orderByDesc(OrderDelivery::getCreateTime);

        // 5. 执行分页查询
        Page<OrderDelivery> entityPage = orderDeliveryMapper.selectPage(page, wrapper);

        // 6. 转换为前端专用VO，并设置状态名称
        Page<OrderDeliveryVO> voPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );

        List<OrderDeliveryVO> voList = entityPage.getRecords().stream()
                .map(item -> {
                    OrderDeliveryVO vo = new OrderDeliveryVO();
                    BeanUtil.copyProperties(item, vo);

                    // 设置状态名称
                    OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(item.getOrderStatus());
                    vo.setStatusName(statusEnum == null ? "未知状态" : statusEnum.getDesc());

                    return vo;
                }).collect(Collectors.toList());

        voPage.setRecords(voList);

        return Result.success(voPage);
    }



    /**
     * 确认发货
     * 业务规则：
     * 1. 订单必须存在
     * 2. 订单必须是【已支付】状态
     * 3. 更新为【已发货】，填入物流单号、备注
     *
     * @param id  订单ID
     * @param dto 发货参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> confirmDelivery(Long id, OrderDeliveryDTO dto) {
        // 1. 查询订单
        OrderDelivery order = getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 2. 校验状态：仅已支付可发货
        if (!OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            return Result.fail("仅【已支付】订单可发货");
        }

        // 3. 执行发货更新
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        order.setLogisticsNo(dto.getLogisticsNo());
        if (StrUtil.isNotBlank(dto.getRemark())) {
            order.setRemark(dto.getRemark());
        }
        order.setUpdateTime(LocalDateTime.now());

        updateById(order);

        return Result.success("发货成功");
    }

}




