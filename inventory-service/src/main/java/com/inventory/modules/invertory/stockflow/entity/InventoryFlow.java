package com.inventory.modules.invertory.stockflow.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水表实体，对应表 {@code inventory_flow}。
 * <p>
 * 每次库存变动（增/减/锁/解锁）都应写入一条流水，便于审计与排查。
 * 主键由 Hutool 雪花算法在 {@code StockServiceImpl#writeFlow} 中生成。
 * </p>
 */
@TableName(value = "inventory_flow")
@Data
public class InventoryFlow {

    /** 流水主键 */
    @TableId
    private Long id;

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 变动前库存（锁定场景下记的是 lock_stock） */
    private Integer beforeStock;

    /** 变动数量（正增负减） */
    private Integer changeStock;

    /** 变动后库存 */
    private Integer afterStock;

    /** 操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整 */
    private Integer operateType;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 业务单号（入库单号 / 订单号 / 物流单号等） */
    private String bizNo;

    /** 备注 */
    private String remark;

    /** 排序号，数字越小越靠前 */
    private Integer sort;
}
