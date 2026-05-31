package com.inventory.modules.order.orderinfo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;

/**
* @author 95349
* @description 针对表【order_info(订单信息表)】的数据库操作Service
* @createDate 2026-05-31 11:09:26
*/
public interface OrderInfoService extends IService<OrderInfo> {
    /**
     * 订单分页列表
     */
    Result<?> pageOrderList(String orderNo,
                            String goodsName,
                            String orderStatus,
                            String startTime,
                            String endTime,
                            Long pageNum,
                            Long pageSize);

    /**
     * 新建订单
     */
    Result<?> createOrder(OrderInfoDTO dto);

    /**
     * 确认支付
     */
    Result<?> payOrder(Long id);

    /**
     * 取消订单
     */
    Result<?> cancelOrder(Long id);

}
