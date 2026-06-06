package com.inventory.modules.order.orderinfo.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



/**
 * 订单信息控制器
 * 路由前缀：/order/info
 * 权限前缀：order:info:xxx
 * @author 95349
 * @date 2026-05-31
 */
@RestController
@RequestMapping("/order/info")
@RequiredArgsConstructor
@Validated // 开启参数校验
public class OrderInfoController {

    private final OrderInfoService orderInfoService;

    /**
     * 订单分页列表查询
     * 权限：order:info:list
     * @param orderNo 订单编号
     * @param goodsName 商品名称
     * @param orderStatus 订单状态
     * @param startTime 创建开始时间（yyyy-MM-dd HH:mm:ss）
     * @param endTime 创建结束时间（yyyy-MM-dd HH:mm:ss）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页结果
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
                startTime, endTime, pageNum, pageSize
        );
    }

    /**
     * 新建订单
     * 权限：order:info:add
     * @param dto 订单创建参数（已做参数校验）
     * @return 创建结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('order:info:add')")
    public Result<?> add(@Valid @RequestBody OrderInfoDTO dto) {
        return orderInfoService.createOrder(dto);
    }

    /**
     * 确认支付（仅待支付订单）
     * 权限：order:info:pay
     * @param id 订单主键ID
     * @return 支付结果
     */
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('order:info:pay')")
    public Result<?> pay(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.payOrder(id);
    }


    /**
     * 确认支付（仅待支付订单）
     * 权限：order:info:pay
     * @param id 订单主键ID
     * @return 支付结果
     */
    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('order:info:receive')")
    public Result<?> receive(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.receiveOrder(id);
    }

    /**
     * 取消订单（待支付、已支付订单）
     * 权限：order:info:cancel
     * @param id 订单主键ID
     * @return 取消结果
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:info:cancel')")
    public Result<?> cancel(@NotNull(message = "订单ID不能为空") @PathVariable Long id) {
        return orderInfoService.cancelOrder(id);
    }
}