package com.inventory.modules.order.orderdelivery.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.enums.OrderStatusEnum;
import com.inventory.common.response.Result;
import com.inventory.modules.goods.product.entity.GoodsProduct;
import com.inventory.modules.goods.product.mapper.GoodsProductMapper;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;
import com.inventory.modules.order.orderdelivery.service.OrderDeliveryService;
import com.inventory.modules.order.orderdelivery.mapper.OrderDeliveryMapper;
import com.inventory.modules.order.orderdelivery.vo.OrderDeliveryVO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
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
     * 订单 Mapper
     */
    private final OrderInfoMapper orderInfoMapper;

    /**
     * 商品 Mapper
     */
    private final GoodsProductMapper  goodsProductMapper;
    /**
     * 库存 Mapper
     */
    private final InventoryStockMapper inventoryStockMapper;

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
     * 2. 仅允许【待发货】订单发货
     * 3. 扣减真实库存（lockStock - 数量，stock - 数量）
     * 4. 更新【发货表】为【已发货】状态
     * 5. 更新【主订单表】为【已发货】状态
     * 6. 记录物流单号、备注
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> confirmDelivery(Long id, OrderDeliveryDTO dto) {
        // 1. 查询【发货单】
        OrderDelivery deliveryOrder = getById(id);
        if (deliveryOrder == null) {
            return Result.fail("发货单不存在");
        }

        // 2. 只能【待发货】才能发货
        if (!OrderStatusEnum.PAID.getCode().equals(deliveryOrder.getOrderStatus())) {
            return Result.fail("仅【待发货】订单可以发货");
        }

        // ===================== 3. 扣减真实库存（最终业务闭环） =====================
        stockService.decreaseStockFlow(deliveryOrder.getGoodsId(), deliveryOrder.getBuyQty(),dto.getLogisticsNo());

        // ===================== 4. 更新【发货表】→ 已发货 =====================
        deliveryOrder.setLogisticsNo(dto.getLogisticsNo());
        deliveryOrder.setRemark(dto.getRemark());
        deliveryOrder.setOrderStatus(OrderStatusEnum.SHIPPED.getCode()); // 2
        deliveryOrder.setUpdateTime(LocalDateTime.now());
        updateById(deliveryOrder); // 更新发货表

        // ===================== 5. 更新【主订单表 order_info】→ 已发货（你要的关键！） =====================
        // 根据 orderNo 查询主订单
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                Wrappers.lambdaQuery(OrderInfo.class)
                        .eq(OrderInfo::getOrderNo, deliveryOrder.getOrderNo())
        );
        if (orderInfo != null) {
            orderInfo.setOrderStatus(OrderStatusEnum.SHIPPED.getCode()); // 主订单 → 2 已发货
            orderInfo.setLogisticsNo(dto.getLogisticsNo());
            orderInfo.setUpdateTime(LocalDateTime.now());
            orderInfoMapper.updateById(orderInfo);
        }

        return Result.success("发货成功，订单已同步更新为【已发货】");
    }

}