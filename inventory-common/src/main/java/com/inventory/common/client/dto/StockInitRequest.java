package com.inventory.common.client.dto;

import lombok.Data;

/**
 * 库存初始化请求 DTO（跨服务）。
 * <p>
 * 供 <b>platform-service</b> 在「新增商品」成功后，调用 inventory-service
 * {@code POST /inventory/internal/init-stock}，在库存库中写入对应 {@code inventory_stock} 记录。
 * </p>
 * <p>
 * 字段说明与默认值约定（由库存服务落地）：
 * <ul>
 *   <li>{@code goodsId} — 必填，与商品主键一致</li>
 *   <li>{@code goodsName} / {@code categoryName} — 冗余展示字段</li>
 *   <li>{@code stock} — 可选，缺省按 0</li>
 *   <li>{@code stockWarn} — 可选，缺省按 10</li>
 *   <li>{@code lockStock} — 可选，缺省按 0</li>
 * </ul>
 * 接口侧对同一 {@code goodsId} 幂等：已存在库存记录则直接成功，不重复插入。
 * </p>
 */
@Data
public class StockInitRequest {

    /** 商品主键 ID（必填） */
    private Long goodsId;

    /** 商品名称（冗余，便于库存列表展示） */
    private String goodsName;

    /** 分类名称（冗余） */
    private String categoryName;

    /** 初始账面库存；可选，库存服务默认 0 */
    private Integer stock;

    /** 库存预警阈值；可选，库存服务默认 10 */
    private Integer stockWarn;

    /** 初始锁定库存；可选，库存服务默认 0 */
    private Integer lockStock;
}
