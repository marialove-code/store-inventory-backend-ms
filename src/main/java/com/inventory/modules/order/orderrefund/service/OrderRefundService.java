package com.inventory.modules.order.orderrefund.service;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderrefund.dto.OrderRefundApplyDTO;
import com.inventory.modules.order.orderrefund.dto.OrderRefundDTO;
import com.inventory.modules.order.orderrefund.entity.OrderRefund;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 95349
* @description 针对表【order_refund】的数据库操作Service
* @createDate 2026-05-29 19:04:18
*/
public interface OrderRefundService extends IService<OrderRefund> {


    /**
     * 退款订单分页列表
     */
    Result<?> pageRefundList(Long pageNum, Long pageSize, String orderNo, String refundStatus, String startTime, String endTime);


    /**
     * 发起退款申请
     */
    Result<?> applyRefund(OrderRefundApplyDTO dto);

    /**
     * 通过退款申请
     */
    Result<?> approveRefund(Long id, OrderRefundDTO dto);

    /**
     * 拒绝退款申请
     */
    Result<?> rejectRefund(Long id, OrderRefundDTO dto);
}
