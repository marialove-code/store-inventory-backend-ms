package com.inventory.modules.order.orderrefund.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderrefund.dto.OrderRefundApplyDTO;
import com.inventory.modules.order.orderrefund.dto.OrderRefundDTO;
import com.inventory.modules.order.orderrefund.service.OrderRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 订单退款管理 控制层
 */
@RestController
@RequestMapping("/order/refund")
@RequiredArgsConstructor
public class OrderRefundController {

    private final OrderRefundService orderRefundService;

    /**
     * 退款订单分页列表
     * 权限：order:refund:list
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('order:refund:list')")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String refundStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return orderRefundService.pageRefundList(pageNum, pageSize, orderNo, refundStatus, startTime, endTime);
    }


    /**
     * 发起退款申请
     * 权限：order:refund:apply
     */
    @PostMapping
    @PreAuthorize("hasAuthority('order:refund:apply')")
    public Result<?> apply(@RequestBody OrderRefundApplyDTO dto) {
        return orderRefundService.applyRefund(dto);
    }

    /**
     * 通过退款申请
     * 权限：order:refund:approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<?> approve(@PathVariable Long id, @RequestBody(required = false) OrderRefundDTO dto) {
        return orderRefundService.approveRefund(id, dto);
    }

    /**
     * 拒绝退款申请
     * 权限：order:refund:reject
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('order:refund:reject')")
    public Result<?> reject(@PathVariable Long id, @RequestBody(required = false) OrderRefundDTO dto) {
        return orderRefundService.rejectRefund(id, dto);
    }
}