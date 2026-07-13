package com.inventory.modules.invertory.stockflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水返回 VO（列表 / 详情）。
 */
@Data
public class InventoryFlowVO {

    private String id;
    private String goodsId;
    private String goodsName;
    private Integer beforeStock;
    private Integer changeStock;
    private Integer afterStock;

    /** 操作类型枚举：RECEIPT / OUTBOUND / LOCK / UNLOCK / ADJUST */
    private String operateType;

    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private String bizNo;
    private String remark;

    /** 操作类型中文名 */
    private String operateTypeName;
}
