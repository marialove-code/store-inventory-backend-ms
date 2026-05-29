package com.inventory.modules.invertory.stockflow.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 库存流水表
 * @TableName inventory_flow
 */
@TableName(value ="inventory_flow")
@Data
public class InventoryFlow {
    /**
     * 流水主键
     */
    @TableId
    private Long id;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 变动前库存
     */
    private Integer beforeStock;

    /**
     * 变动数量（正增负减）
     */
    private Integer changeStock;

    /**
     * 变动后库存
     */
    private Integer afterStock;

    /**
     * 操作类型：1-入库 2-出库 3-锁定 4-解锁 5-调整
     */
    private Integer operateType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 业务单号
     */
    private String bizNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序号，数字越小越靠前
     */
    private Integer sort;
}