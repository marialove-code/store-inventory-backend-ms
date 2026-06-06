package com.inventory.modules.order.orderinfo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import jakarta.validation.constraints.NotNull;

/**
 * 订单信息服务接口
 * 定义订单相关的业务逻辑接口
 * @author 95349
 * @date 2026-05-31
 */
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 订单分页列表查询
     * @param orderNo 订单编号（精确匹配）
     * @param goodsName 商品名称（模糊匹配）
     * @param orderStatus 订单状态编码
     * @param startTime 创建时间开始范围（格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 创建时间结束范围（格式：yyyy-MM-dd HH:mm:ss）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页结果（含订单列表及状态名称）
     */
    Result<?> pageOrderList(String orderNo,
                            String goodsName,
                            String orderStatus,
                            String startTime,
                            String endTime,
                            Long pageNum,
                            Long pageSize);

    /**
     * 新建订单
     * @param dto 订单创建入参
     * @return 创建结果
     */
    Result<?> createOrder(OrderInfoDTO dto);

    /**
     * 订单支付（仅待支付订单可操作）
     * @param id 订单主键ID
     * @return 支付结果
     */
    Result<?> payOrder(Long id);

    /**
     * 取消订单（仅待支付、已支付订单可操作）
     * @param id 订单主键ID
     * @return 取消结果
     */
    Result<?> cancelOrder(Long id);
    /**
     * 确认收货（已发货）
     * @param id 订单主键ID
     * @return 支付结果
     */
    Result<?> receiveOrder(@NotNull(message = "订单ID不能为空") Long id);
}