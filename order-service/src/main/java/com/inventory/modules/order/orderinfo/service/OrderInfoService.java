package com.inventory.modules.order.orderinfo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import jakarta.validation.constraints.NotNull;

/**
 * 订单信息服务接口。
 */
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 订单分页多条件查询。
     */
    Result<?> pageOrderList(String orderNo,
                            String goodsName,
                            String orderStatus,
                            String startTime,
                            String endTime,
                            Long pageNum,
                            Long pageSize);

    /**
     * 新建订单（远程锁库存后再落库）。
     */
    Result<?> createOrder(OrderInfoDTO dto);

    /**
     * 订单支付（仅待支付可操作）。
     */
    Result<?> payOrder(Long id);

    /**
     * 取消订单（仅待支付/已支付可操作；先远程解锁再改状态）。
     */
    Result<?> cancelOrder(Long id);

    /**
     * 确认收货（已发货 → 已完成）。
     */
    Result<?> receiveOrder(@NotNull(message = "订单ID不能为空") Long id);
}
