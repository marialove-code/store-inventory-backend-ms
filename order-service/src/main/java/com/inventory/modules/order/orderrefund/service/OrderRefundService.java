package com.inventory.modules.order.orderrefund.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderrefund.dto.OrderRefundApplyDTO;
import com.inventory.modules.order.orderrefund.dto.OrderRefundDTO;
import com.inventory.modules.order.orderrefund.entity.OrderRefund;

/**
 * 退款/退货 Service。
 */
public interface OrderRefundService extends IService<OrderRefund> {

    Result<?> pageRefundList(Long pageNum, Long pageSize, String orderNo,
                             String refundStatus, String startTime, String endTime);

    Result<?> applyRefund(OrderRefundApplyDTO dto);

    Result<?> approveRefund(Long id, OrderRefundDTO dto);

    Result<?> rejectRefund(Long id, OrderRefundDTO dto);
}
