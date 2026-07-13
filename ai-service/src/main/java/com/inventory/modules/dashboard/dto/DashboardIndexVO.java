package com.inventory.modules.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页/看板聚合数据（从单体迁入，供 AI 洞察使用）。
 */
@Data
public class DashboardIndexVO {

    private StatsVO stats;
    private List<SalesTrendVO> salesTrend;
    private List<HotGoodsVO> hotTop5;
    private List<CategoryRatioVO> categoryRatio;
    private MonitorVO monitor;

    @Data
    public static class StatsVO {
        private BigDecimal todaySales;
        private BigDecimal todaySalesRatio;
        private Integer todayOrders;
        private BigDecimal todayOrdersRatio;
        private Integer totalStock;
        private Integer warnStockCount;
    }

    @Data
    public static class SalesTrendVO {
        private String date;
        private BigDecimal amount;
    }

    @Data
    public static class HotGoodsVO {
        private String goodsName;
        private Integer salesNum;
    }

    @Data
    public static class CategoryRatioVO {
        private String categoryName;
        private BigDecimal percent;
    }

    @Data
    public static class MonitorVO {
        private String redisStatus;
        private Double redisMemoryUsage;
        private Integer onlineUserCount;
        private Integer todayApiCount;
    }
}
