package com.inventory.modules.order.orderinfo.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import com.inventory.order.config.SentinelResourceNames;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单信息控制器。
 * <p>
 * 路由前缀：/order/info。微服务阶段去掉 {@code @RequiresPerm}，鉴权后续由网关/平台服务承担。
 * 下单 / 取消挂 Sentinel 流控（资源 {@code orderCreate} / {@code orderCancel}）；探活 ping 仍保留作参考。
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

    /** 新建订单（真实业务入口 + 流控） */
    @PostMapping("/add")
    @SentinelResource(
            value = SentinelResourceNames.ORDER_CREATE,
            blockHandler = "addBlockHandler"
    )
    public Result<?> add(@Valid @RequestBody OrderInfoDTO dto) {
        return orderInfoService.createOrder(dto);
    }

    /**
     * 下单被限流/熔断时的兜底（须与原方法参数一致，末参 BlockException）。
     */
    public Result<?> addBlockHandler(OrderInfoDTO dto, BlockException ex) {
        return Result.fail("下单触发 Sentinel 防护（资源 orderCreate），请稍后再试："
                + ex.getClass().getSimpleName());
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

    /** 取消订单（真实业务入口 + 流控；内部会远程 unlock 还库存） */
    @PutMapping("/{id}/cancel")
    @SentinelResource(
            value = SentinelResourceNames.ORDER_CANCEL,
            blockHandler = "cancelBlockHandler"
    )
    public Result<?> cancel(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.cancelOrder(id);
    }

    public Result<?> cancelBlockHandler(Long id, BlockException ex) {
        return Result.fail("取消订单触发 Sentinel 防护（资源 orderCancel），请稍后再试："
                + ex.getClass().getSimpleName());
    }
}
