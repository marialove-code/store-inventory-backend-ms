package com.inventory.modules.order.orderdelivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;

/**
 * 发货管理 Service。
 */
public interface OrderDeliveryService extends IService<OrderDelivery> {

    /**
     * 发货单分页列表。
     */
    Result<?> pageDeliveryList(String orderNo, String startTime, String endTime, Long pageNum, Long pageSize);

    /**
     * 确认发货（远程 decreaseFlow + 更新发货单/主订单状态）。
     */
    Result<?> confirmDelivery(Long id, OrderDeliveryDTO dto);
}
