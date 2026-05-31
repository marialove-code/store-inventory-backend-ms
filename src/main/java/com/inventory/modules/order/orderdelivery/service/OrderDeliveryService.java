package com.inventory.modules.order.orderdelivery.service;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.entity.OrderDelivery;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 95349
* @description 针对表【order_delivery】的数据库操作Service
* @createDate 2026-05-29 19:04:18
*/
public interface OrderDeliveryService extends IService<OrderDelivery> {


    /**
     * 待发货订单分页列表
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param orderNo   订单号
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页结果
     */
    Result<?> pageDeliveryList(Long pageNum, Long pageSize, String orderNo, String startTime, String endTime);



    /**
     * 确认订单发货
     *
     * @param id  订单ID
     * @param dto 发货参数
     * @return 操作结果
     */
    Result<?> confirmDelivery(Long id, OrderDeliveryDTO dto);

}
