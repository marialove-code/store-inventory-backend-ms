package com.inventory.modules.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryPercentVO {
    //分类名称
    private String categoryName;
    //分类销售总额
    private BigDecimal totalAmount;
}