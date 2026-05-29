package com.inventory.modules.invertory.stockout.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName inventory_out
 */
@TableName(value ="inventory_out")
@Data
public class InventoryOut {
    /**
     * 出库单主键
     */
    @TableId
    private Long id;

    /**
     * 出库单号
     */
    private String outboundNo;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 出库数量
     */
    private Integer outboundQty;

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