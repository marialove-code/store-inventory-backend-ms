package com.inventory.modules.invertory.stockin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入库单实体，对应表 {@code inventory_in}。
 */
@TableName(value = "inventory_in")
@Data
public class InventoryIn {

    /** 入库单主键 */
    @TableId
    private Long id;

    /** 入库单号 */
    private String receiptNo;

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称（冗余，便于列表展示） */
    private String goodsName;

    /** 入库数量 */
    private Integer receiptQty;

    /** 操作人 */
    private String operator;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;

    /** 排序号 */
    private Integer sort;
}
