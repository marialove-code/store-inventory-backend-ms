package com.inventory.modules.dashboard.service.impl;

import com.inventory.modules.dashboard.dto.DashboardIndexVO;
import com.inventory.modules.dashboard.service.DashboardIndexService;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 服务内看板数据聚合（同库直连，不依赖单体 Dashboard 整链）。
 * <p>
 * 订单侧用 {@link JdbcTemplate}；库存侧复用 {@link InventoryStockMapper}；
 * Redis/在线人数本阶段占位（后续可由 platform 提供）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardIndexServiceImpl implements DashboardIndexService {

    private final JdbcTemplate jdbcTemplate;
    private final InventoryStockMapper inventoryStockMapper;

    @Override
    public DashboardIndexVO getDashboardIndexData(String period) {
        DashboardIndexVO dto = new DashboardIndexVO();

        DashboardIndexVO.StatsVO stats = new DashboardIndexVO.StatsVO();
        stats.setTodaySales(queryTodaySales());
        stats.setTodayOrders(queryTodayOrders());
        Integer totalStock = inventoryStockMapper.getTotalStock();
        Integer warnStock = inventoryStockMapper.getWarnStockCount();
        stats.setTotalStock(totalStock != null ? totalStock : 0);
        stats.setWarnStockCount(warnStock != null ? warnStock : 0);
        stats.setTodaySalesRatio(BigDecimal.ZERO);
        stats.setTodayOrdersRatio(BigDecimal.ZERO);
        dto.setStats(stats);

        int days = 7;
        if ("30d".equals(period)) {
            days = 30;
        } else if ("90d".equals(period)) {
            days = 90;
        }
        dto.setSalesTrend(querySalesTrend(days));
        dto.setHotTop5(queryHotTop5());
        dto.setCategoryRatio(List.of());

        // 本阶段无 Redis：监控字段给安全默认值，避免 AI 洞察空指针
        DashboardIndexVO.MonitorVO monitor = new DashboardIndexVO.MonitorVO();
        monitor.setRedisStatus("SKIPPED");
        monitor.setRedisMemoryUsage(0D);
        monitor.setOnlineUserCount(0);
        monitor.setTodayApiCount(0);
        dto.setMonitor(monitor);

        return dto;
    }

    private BigDecimal queryTodaySales() {
        try {
            BigDecimal v = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(SUM(order_amount), 0)
                    FROM order_info
                    WHERE create_time >= CURRENT_DATE
                      AND create_time < CURRENT_DATE + INTERVAL '1 day'
                      AND order_status IN (1, 2, 3)
                    """,
                    BigDecimal.class);
            return v != null ? v : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("查询今日销售额失败: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Integer queryTodayOrders() {
        try {
            Integer v = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(COUNT(*), 0)
                    FROM order_info
                    WHERE create_time >= CURRENT_DATE
                      AND create_time < CURRENT_DATE + INTERVAL '1 day'
                      AND order_status IN (1, 2, 3)
                    """,
                    Integer.class);
            return v != null ? v : 0;
        } catch (Exception e) {
            log.warn("查询今日订单数失败: {}", e.getMessage());
            return 0;
        }
    }

    private List<DashboardIndexVO.SalesTrendVO> querySalesTrend(int days) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT TO_CHAR(create_time, 'MM-DD') AS date,
                           COALESCE(SUM(order_amount), 0) AS amount
                    FROM order_info
                    WHERE order_status IN (1, 2, 3)
                      AND create_time >= CURRENT_DATE - INTERVAL '1 day' * ?
                    GROUP BY TO_CHAR(create_time, 'MM-DD'), DATE(create_time)
                    ORDER BY DATE(create_time) ASC
                    """,
                    days);
            List<DashboardIndexVO.SalesTrendVO> list = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                DashboardIndexVO.SalesTrendVO vo = new DashboardIndexVO.SalesTrendVO();
                vo.setDate(String.valueOf(row.get("date")));
                Object amount = row.get("amount");
                vo.setAmount(amount instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(amount)));
                list.add(vo);
            }
            return list;
        } catch (Exception e) {
            log.warn("查询销售趋势失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<DashboardIndexVO.HotGoodsVO> queryHotTop5() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT goods_name AS goodsName,
                           COALESCE(SUM(buy_qty), 0) AS salesNum
                    FROM order_info
                    WHERE order_status IN (1, 2, 3)
                    GROUP BY goods_id, goods_name
                    ORDER BY salesNum DESC
                    LIMIT 5
                    """);
            List<DashboardIndexVO.HotGoodsVO> list = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                DashboardIndexVO.HotGoodsVO vo = new DashboardIndexVO.HotGoodsVO();
                vo.setGoodsName(String.valueOf(row.get("goodsname") != null ? row.get("goodsname") : row.get("goodsName")));
                Object sales = row.get("salesnum") != null ? row.get("salesnum") : row.get("salesNum");
                vo.setSalesNum(sales == null ? 0 : ((Number) sales).intValue());
                list.add(vo);
            }
            return list;
        } catch (Exception e) {
            log.warn("查询热销 TOP5 失败: {}", e.getMessage());
            return List.of();
        }
    }
}
