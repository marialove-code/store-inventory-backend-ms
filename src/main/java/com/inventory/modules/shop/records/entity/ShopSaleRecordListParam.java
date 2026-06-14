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

    /**
     * 售卖年份（可选）
     */
    private Integer saleYear;

    /**
     * 售卖月份 1-12（可选，需配合 saleYear）
     */
    private Integer saleMonth;

    /**
     * 售卖日 1-31（可选，需配合 saleYear、saleMonth）
     */
    private Integer saleDay;
}