package com.inventory.common.client.dto;

import lombok.Data;

/**
 * 写入库存流水请求（跨服务 DTO）。
 * <p>
 * 对应 inventory-service {@code POST /inventory/internal/write-flow}，
 * 供 order-service V3 异步补写流水使用。
 * </p>
 */
@Data
public class WriteFlowRequest {

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 变动前库存（锁定场景下为变动前的 lock_stock） */
    private Integer beforeStock;

    /** 变动数量（正数增加，负数减少） */
    private Integer changeStock;

    /** 变动后库存 */
    private Integer afterStock;

    /** 操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整 */
    private Integer operateType;

    /** 业务单号（如订单号） */
    private String bizNo;

    /** 备注 */
    private String remark;
}
