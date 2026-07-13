package com.inventory.modules.shop.dashboard.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 门店首页看板返回VO
 */
@Data
public class DashboardShopVO {
    //====================今日概览====================
    /** 今日销售额 */
    private BigDecimal todaySaleAmount = BigDecimal.ZERO;
    /** 今日总利润 */
    private BigDecimal todayProfit = BigDecimal.ZERO;
    /** 今日订单单数 */
    private Integer todayOrderCount = 0;
    /** 今日出库总件数 */
    private Integer todaySaleQty = 0;

    //====================收入统计（月/季/年）====================
    /** 本月累计销售额 */
    private BigDecimal monthSaleAmount = BigDecimal.ZERO;
    /** 本季度累计销售额 */
    private BigDecimal quarterSaleAmount = BigDecimal.ZERO;
    /** 本年度累计销售额 */
    private BigDecimal yearSaleAmount = BigDecimal.ZERO;

    //====================热销TOP5商品====================
    private List<ShopHotProductVO> hotTop5;

    //====================库存预警列表（库存<预警值）====================
    private List<ShopStockWarnVO> stockWarnList;
}

