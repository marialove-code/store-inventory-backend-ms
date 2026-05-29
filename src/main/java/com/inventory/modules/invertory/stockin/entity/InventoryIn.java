package com.inventory.modules.invertory.stockin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName inventory_in
 */
@TableName(value ="inventory_in")
@Data
public class InventoryIn {
    /**
     * 入库单主键
     */
    @TableId
    private Long id;

    /**
     * 入库单号
     */
    private String receiptNo;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 入库数量
     */
    private Integer receiptQty;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序号，数字越小越靠前
     */
    private Integer sort;
}