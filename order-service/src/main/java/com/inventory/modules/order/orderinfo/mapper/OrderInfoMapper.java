package com.inventory.modules.order.orderinfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;

/**
 * 订单信息表 Mapper。
 * <p>
 * P2 仅保留 BaseMapper CRUD；单体中的仪表盘统计 SQL 依赖商品/仪表盘模块，暂不迁入。
 * </p>
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}
