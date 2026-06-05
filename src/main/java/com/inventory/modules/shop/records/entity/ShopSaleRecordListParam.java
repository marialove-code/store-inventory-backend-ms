package com.inventory.modules.shop.records.entity;

import lombok.Data;

/**
 * 销售流水 - 分页查询参数
 */
@Data
public class ShopSaleRecordListParam {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 搜索关键词（商品名称）
     */
    private String keyword;

    /**
     * 时间筛选：today/week/all
     */
    private String timeFilter;
}