package com.inventory.modules.invertory.stockflow.dto;

import lombok.Data;

/**
 * 库存流水查询 / 导出请求参数。
 */
@Data
public class InventoryFlowQueryDTO {

    /** 商品名称（模糊） */
    private String goodsName;

    /** 操作类型：RECEIPT / OUTBOUND / LOCK / UNLOCK / ADJUST */
    private String operateType;

    private String startTime;
    private String endTime;
    private Long pageNum;
    private Long pageSize;
}
