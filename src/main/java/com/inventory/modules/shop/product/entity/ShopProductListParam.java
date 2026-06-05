package com.inventory.modules.shop.product.entity;

import lombok.Data;

/**
 * 商品配件 - 分页查询参数
 */
@Data
public class ShopProductListParam {

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
}