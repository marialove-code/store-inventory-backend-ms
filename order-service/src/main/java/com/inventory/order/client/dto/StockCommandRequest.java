package com.inventory.order.client.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存命令式 API 请求体（订单侧本地 DTO）。
 * <p>
 * 字段与 inventory-service 的 {@code StockCommandRequest} 对齐，
 * <b>刻意不依赖 inventory-service 模块</b>，避免订单服务反向依赖库存实现。
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

    /** 业务单号（可选，如订单号；写入库存流水 biz_no） */
    private String bizNo;

    /** 单据号（可选，如物流单号；与 bizNo 二选一） */
    private String receiptNo;

    /**
     * 便捷构造：goodsId + qty + bizNo。
     */
    public static StockCommandRequest of(Long goodsId, Integer qty, String bizNo) {
        StockCommandRequest req = new StockCommandRequest();
        req.setGoodsId(goodsId);
        req.setQty(qty);
        req.setBizNo(bizNo);
        return req;
    }

    /**
     * 便捷构造：goodsId + qty（无单号）。
     */
    public static StockCommandRequest of(Long goodsId, Integer qty) {
        return of(goodsId, qty, null);
    }
}
