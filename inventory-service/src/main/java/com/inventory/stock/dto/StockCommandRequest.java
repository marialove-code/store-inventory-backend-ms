package com.inventory.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存命令式 API 通用请求体。
 * <p>
 * 供订单服务后续通过 HTTP 调用：锁库存 / 解锁 / 发货扣减 / 退货回库等。
 * {@code bizNo} 与 {@code receiptNo} 语义相同，按调用场景二选一或同传均可；
 * 服务端优先使用非空的那个写入流水。
 * </p>
 */
@Data
public class StockCommandRequest {

    /** 商品 ID（必填） */
    @NotNull(message = "goodsId 不能为空")
    private Long goodsId;

    /** 变动数量（必填，须 &gt; 0） */
    @NotNull(message = "qty 不能为空")
    @Min(value = 1, message = "qty 必须大于 0")
    private Integer qty;

    /** 业务单号（可选，如订单号；写入 inventory_flow.biz_no） */
    private String bizNo;

    /** 单据号（可选，如入库单号 / 物流单号；与 bizNo 二选一） */
    private String receiptNo;
}
