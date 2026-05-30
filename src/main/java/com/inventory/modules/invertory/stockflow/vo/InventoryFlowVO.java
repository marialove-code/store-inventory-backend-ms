package com.inventory.modules.invertory.stockflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存流水返回VO
 * 用于列表和详情接口
 */
@Data
public class InventoryFlowVO {

    /**
     * 流水主键ID
     */
    private String id;

    /**
     * 商品ID
     */
    private String goodsId;

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
     * 操作类型枚举：RECEIPT/OUTBOUND/LOCK/UNLOCK/ADJUST
     */
    private String operateType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 业务单号
     */
    private String bizNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作类型名称（入库/出库/锁定/解锁/调整）
     */
    private String operateTypeName;
}