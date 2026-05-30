package com.inventory.modules.invertory.stockflow.dto;

import lombok.Data;

/**
 * 库存流水查询DTO
 * 用于列表查询和导出接口
 */
@Data
public class InventoryFlowQueryDTO {

    /**
     * 商品名称（模糊查询）
     */
    private String goodsName;

    /**
     * 操作类型（枚举值：1-入库 2-出库 3-锁定 4-解锁 5-调整）
     */
    private String operateType;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 页码
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;
}