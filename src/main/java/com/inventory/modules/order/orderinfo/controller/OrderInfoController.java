package com.inventory.modules.order.orderinfo.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 订单信息控制层
 * 路由：/order/info
 * 权限：order:info:xxx
 */
@RestController
@RequestMapping("/order/info")
@RequiredArgsConstructor
public class OrderInfoController {

    private final OrderInfoService orderInfoService;

    /**
     * 1. 订单分页列表
     * 权限：order:info:list
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('order:info:list')")
    public Result<?> list(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return orderInfoService.pageOrderList(
                orderNo, goodsName, orderStatus,
                startTime, endTime, pageNum, pageSize);
    }


    /**
     * 2. 新建订单
     * 权限：order:info:add
     */
    @PostMapping
    @PreAuthorize("hasAuthority('order:info:add')")
    public Result<?> add(@RequestBody OrderInfoDTO dto) {
        return orderInfoService.createOrder(dto);
    }

    /**
     * 3. 确认支付（仅待支付）
     * 权限：order:info:pay
     */
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('order:info:pay')")
    public Result<?> pay(@PathVariable Long id) {
        return orderInfoService.payOrder(id);
    }

    /**
     * 4. 取消订单（待支付、已支付）
     * 权限：order:info:cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:info:cancel')")
    public Result<?> cancel(@PathVariable Long id) {
        return orderInfoService.cancelOrder(id);
    }
}