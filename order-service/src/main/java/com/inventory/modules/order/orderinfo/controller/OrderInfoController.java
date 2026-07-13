package com.inventory.modules.order.orderinfo.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单信息控制器。
 * <p>
 * 路由前缀：/order/info。微服务阶段去掉 {@code @RequiresPerm}，鉴权后续由网关/平台服务承担。
 * </p>
 */
@RestController
@RequestMapping("/order/info")
@RequiredArgsConstructor
@Validated
public class OrderInfoController {

    private final OrderInfoService orderInfoService;

    /** 订单分页列表 */
    @GetMapping("/list")
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
                startTime, endTime, pageNum, pageSize
        );
    }

    /** 新建订单 */
    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody OrderInfoDTO dto) {
        return orderInfoService.createOrder(dto);
    }

    /** 确认支付（仅待支付订单） */
    @PutMapping("/{id}/pay")
    public Result<?> pay(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.payOrder(id);
    }

    /** 确认收货 */
    @PutMapping("/{id}/receive")
    public Result<?> receive(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.receiveOrder(id);
    }

    /** 取消订单（待支付、已支付） */
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.cancelOrder(id);
    }
}
