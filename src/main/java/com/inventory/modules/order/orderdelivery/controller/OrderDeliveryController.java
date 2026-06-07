package com.inventory.modules.order.orderdelivery.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.service.OrderDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单发货管理 控制层
 *
 * @author 95349
 * @date 2026-05-31
 */
@RestController
@RequestMapping("/order/delivery")
@RequiredArgsConstructor
public class OrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    /**
     * 待发货订单分页列表
     * 权限：order:delivery:list
     */
    @GetMapping("/list")
    @RequiresPerm("order:delivery:list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return orderDeliveryService.pageDeliveryList(orderNo, startTime, endTime, pageNum, pageSize);
    }

    /**
     * 确认发货
     * 权限：order:delivery:delivery
     */
    @PutMapping("/{id}")
    @RequiresPerm("order:delivery:delivery")
    public Result<?> deliver(
            @PathVariable Long id,
            @RequestBody OrderDeliveryDTO dto) {
        return orderDeliveryService.confirmDelivery(id, dto);
    }
}
