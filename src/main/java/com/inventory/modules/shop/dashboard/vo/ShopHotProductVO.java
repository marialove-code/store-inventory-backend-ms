package com.inventory.modules.shop.dashboard.vo;

import lombok.Data;

/** 热销商品TOP5子VO */
@Data
public class ShopHotProductVO{
    private String productName;
    private Integer saleCount; //销售总数量
}