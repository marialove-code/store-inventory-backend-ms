package com.inventory.modules.order.orderdelivery.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderdelivery.dto.OrderDeliveryDTO;
import com.inventory.modules.order.orderdelivery.service.OrderDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单发货管理控制层。
 * <p>微服务阶段去掉 {@code @RequiresPerm}。</p>
 */
@RestController
@RequestMapping("/order/delivery")
@RequiredArgsConstructor
public class OrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    /** 发货单分页列表 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return orderDeliveryService.pageDeliveryList(orderNo, startTime, endTime, pageNum, pageSize);
    }

    /** 确认发货 */
    @PutMapping("/{id}")
    public Result<?> deliver(@PathVariable Long id, @RequestBody OrderDeliveryDTO dto) {
        return orderDeliveryService.confirmDelivery(id, dto);
    }
}
