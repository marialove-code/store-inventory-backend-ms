package com.inventory.modules.invertory.stock.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实时表实体，对应表 {@code inventory_stock}。
 * <p>
 * 核心字段说明：
 * <ul>
 *   <li>{@code stock} — 实物/账面库存总量</li>
 *   <li>{@code lockStock} — 订单预占锁定数量；可用库存 ≈ stock - lockStock</li>
 *   <li>{@code stockWarn} — 预警阈值；低于此值时 {@code stockStatus=2}</li>
 * </ul>
 * </p>
 */
@TableName(value = "inventory_stock")
@Data
public class InventoryStock {

    /** 库存记录主键 */
    @TableId
    private Long id;

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称（冗余，便于列表展示） */
    private String goodsName;

    /** 分类名称（冗余） */
    private String categoryName;

    /** 库存数量（总量） */
    private Integer stock;

    /** 锁定库存（订单预占） */
    private Integer lockStock;

    /** 库存预警值 */
    private Integer stockWarn;

    /** 库存状态：1-正常 2-预警 3-缺货 */
    private Integer stockStatus;

    /** 排序号，数字越小越靠前 */
    private Integer sort;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 最后入库时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReceiptTime;

    /** 出入库操作标记：1-入库 2-出库 */
    private Integer operateType;
}
