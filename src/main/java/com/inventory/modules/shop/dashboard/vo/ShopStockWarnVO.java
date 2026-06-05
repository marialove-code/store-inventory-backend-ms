package com.inventory.modules.shop.dashboard.vo;

import lombok.Data;

/** 库存预警子VO */
@Data
public class ShopStockWarnVO{
    private String productName;
    private String productCode;
    private String spec;
    private Integer stock;
    private Integer stockWarn;
}