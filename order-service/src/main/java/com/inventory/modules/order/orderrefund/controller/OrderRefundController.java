package com.inventory.modules.order.orderrefund.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderrefund.dto.OrderRefundApplyDTO;
import com.inventory.modules.order.orderrefund.dto.OrderRefundDTO;
import com.inventory.modules.order.orderrefund.service.OrderRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单退款管理控制层。
 * <p>微服务阶段去掉 {@code @RequiresPerm}。</p>
 */
@RestController
@RequestMapping("/order/refund")
@RequiredArgsConstructor
public class OrderRefundController {

    private final OrderRefundService orderRefundService;

    /** 退款单分页列表 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String refundStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return orderRefundService.pageRefundList(pageNum, pageSize, orderNo, refundStatus, startTime, endTime);
    }

    /** 发起退款申请 */
    @PostMapping
    public Result<?> apply(@RequestBody OrderRefundApplyDTO dto) {
        return orderRefundService.applyRefund(dto);
    }

    /** 审核通过 */
    @PutMapping("/{id}/approve")
    public Result<?> approve(@PathVariable Long id, @RequestBody(required = false) OrderRefundDTO dto) {
        return orderRefundService.approveRefund(id, dto);
    }

    /** 审核拒绝 */
    @PutMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id, @RequestBody(required = false) OrderRefundDTO dto) {
        return orderRefundService.rejectRefund(id, dto);
    }
}
