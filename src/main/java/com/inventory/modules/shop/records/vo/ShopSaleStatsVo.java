package com.inventory.modules.shop.records.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售数据统计VO
 */
@Data
public class ShopSaleStatsVo {

    /**
     * 今日营收
     */
    private BigDecimal todayAmount = BigDecimal.ZERO;

    /**
     * 本月营收
     */
    private BigDecimal monthAmount = BigDecimal.ZERO;
}