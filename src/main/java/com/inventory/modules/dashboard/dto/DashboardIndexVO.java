package com.inventory.modules.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页展示数据
 */
@Data
public class DashboardIndexVO {

    // 1. 顶部4指标
    private StatsVO stats;

    // 2. 销售趋势
    private List<SalesTrendVO> salesTrend;

    // 3. 热销TOP5
    private List<HotGoodsVO> hotTop5;

    // 4. 分类占比
    private List<CategoryRatioVO> categoryRatio;

    // 5. 运营监控
    private MonitorVO monitor;

    // ===================== 内部子结构 =====================
    @Data
    public static class StatsVO {
        private BigDecimal todaySales;        // 今日销售额
        private BigDecimal todaySalesRatio;   // 销售额环比
        private Integer todayOrders;          // 今日订单数
        private BigDecimal todayOrdersRatio;  // 订单数环比
        private Integer totalStock;           // 库存总量
        private Integer warnStockCount;       // 预警数量
    }

    @Data
    public static class SalesTrendVO {
        private String date;       // 日期 06-01
        private BigDecimal amount; // 销售额
    }

    @Data
    public static class HotGoodsVO {
        private String goodsName;  // 商品名
        private Integer salesNum;  // 销量
    }

    @Data
    public static class CategoryRatioVO {
        private String categoryName; // 分类名
        private BigDecimal percent;    // 占比值
    }

    @Data
    public static class MonitorVO {
        private String redisStatus;      // Redis状态
        private Double redisMemoryUsage;// 内存使用率
        private Integer onlineUserCount; // 在线人数
        private Integer todayApiCount;   // 今日接口调用量
    }
}