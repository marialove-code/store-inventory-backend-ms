package com.inventory.modules.dashboard.impl;

import com.inventory.modules.dashboard.dto.DashboardIndexVO;
import com.inventory.modules.dashboard.service.DashboardIndexService;
import com.inventory.modules.dashboard.vo.CategoryPercentVO;
import com.inventory.modules.dashboard.vo.HotGoodsVO;
import com.inventory.modules.dashboard.vo.SalesTrendVO;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.monitor.apimonitor.mapper.SysApiMonitorMapper;
import com.inventory.modules.order.orderinfo.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardIndexServiceImpl implements DashboardIndexService {

    private final OrderInfoMapper orderInfoMapper;

    private final InventoryStockMapper inventoryStockMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final SysApiMonitorMapper sysApiMonitorMapper;


    @Override
    public DashboardIndexVO getDashboardIndexData(String period) {
        DashboardIndexVO dto = new DashboardIndexVO();

        // ==============================================
        // 【核心】1. 4个指标卡 → 全部走真实数据库查询
        // ==============================================
        DashboardIndexVO.StatsVO stats = new DashboardIndexVO.StatsVO();

        // 1. 今日销售额
        BigDecimal todaySales = orderInfoMapper.getTodaySalesAmount();
        stats.setTodaySales(todaySales);

        // 2. 今日订单数
        Integer todayOrders = orderInfoMapper.getTodayOrderCount();
        stats.setTodayOrders(todayOrders);

        // 3. 库存总量
        Integer totalStock = inventoryStockMapper.getTotalStock();
        stats.setTotalStock(totalStock);

        // 4. 库存预警数
        Integer warnStock = inventoryStockMapper.getWarnStockCount();
        stats.setWarnStockCount(warnStock);

        // 环比先给默认 0，后面我再带你写
        stats.setTodaySalesRatio(BigDecimal.ZERO);
        stats.setTodayOrdersRatio(BigDecimal.ZERO);

        dto.setStats(stats);

        // ======================
        // 2. 销售趋势（真实数据）
        // ======================
        int days = 7;
        if ("30d".equals(period)) days = 30;
        if ("90d".equals(period)) days = 90;

        List<SalesTrendVO> salesTrendVO = orderInfoMapper.getSalesTrend(days);
        List<DashboardIndexVO.SalesTrendVO> salesTrend = new ArrayList<>();

        for (SalesTrendVO vo : salesTrendVO) {
            DashboardIndexVO.SalesTrendVO bean = new DashboardIndexVO.SalesTrendVO();
            bean.setDate((String) vo.getDate());
            bean.setAmount((BigDecimal) vo.getAmount());
            salesTrend.add(bean);
        }
        dto.setSalesTrend(salesTrend);

        // ======================
        // 3. 热销 TOP5（真实数据）
        // ======================
        List<HotGoodsVO> hotGoodsTop5 = orderInfoMapper.getHotGoodsTop5();
        List<DashboardIndexVO.HotGoodsVO> hotTop5 = new ArrayList<>();

        //热销TOP5循环
        for(HotGoodsVO vo : hotGoodsTop5){
            DashboardIndexVO.HotGoodsVO item=new DashboardIndexVO.HotGoodsVO();
            item.setGoodsName(vo.getGoodsName());
            item.setSalesNum(vo.getSalesNum());
            hotTop5.add(item);
        }
        dto.setHotTop5(hotTop5);

        // ==============================================
        // 4. 分类占比（假数据，下一步写）
        // ==============================================

        List<CategoryPercentVO> categoryList = orderInfoMapper.getCategoryPercent();
        List<DashboardIndexVO.CategoryRatioVO> categoryData = new ArrayList<>();

//先算出全部总金额，用来计算百分比
        BigDecimal allTotal = categoryList.stream()
                .map(CategoryPercentVO::getTotalAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        for(CategoryPercentVO vo : categoryList){
            DashboardIndexVO.CategoryRatioVO item = new DashboardIndexVO.CategoryRatioVO();
            item.setCategoryName(vo.getCategoryName());
            item.setPercent(vo.getTotalAmount());
            //计算占比，保留两位小数
            if(allTotal.compareTo(BigDecimal.ZERO) > 0){
                BigDecimal percent = vo.getTotalAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(allTotal,2,BigDecimal.ROUND_HALF_UP);
                item.setPercent(percent);
            }else{
                item.setPercent(BigDecimal.ZERO);
            }
            categoryData.add(item);
        }
        dto.setCategoryRatio(categoryData);

        // ======================
        // 5. 运营概览（真实数据）
        // ======================
        DashboardIndexVO.MonitorVO monitor = new DashboardIndexVO.MonitorVO();

       // 1. Redis 状态 + 内存使用率
        try {
            RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();
            Properties memoryInfo = connection.info("memory");

            Properties info = connection.info();
            // 已使用内存
            long usedMemory = Long.parseLong(info.getProperty("used_memory", "0"));
            double usedMemoryMb = usedMemory / 1024D / 1024D;

            // 最大内存
            long maxMemory = Long.parseLong(memoryInfo.getProperty("maxmemory", "0"));
            double maxMemoryMb = maxMemory <= 0 ? 0 : maxMemory / 1024D / 1024D;

            // 内存使用率 保留2位小数
            if (usedMemoryMb > 0) {
                double rate = usedMemoryMb * 100 / maxMemoryMb;
                monitor.setRedisMemoryUsage(Math.round(rate * 100D) / 100D);
            } else {
                monitor.setRedisMemoryUsage(0D);
            }
            monitor.setRedisStatus("正常");
        } catch (Exception e) {
            monitor.setRedisStatus("异常");
            monitor.setRedisMemoryUsage(Double.valueOf(0));
        }

     // 2. 在线人数（从Redis获取）
        Integer onlineCount = getOnlineUserCount();
        monitor.setOnlineUserCount(onlineCount);

     // 3. 今日接口调用量
        Integer todayApiCount = sysApiMonitorMapper.countTodayApiRequests();
        monitor.setTodayApiCount(todayApiCount == null ? 0 : todayApiCount);

        dto.setMonitor(monitor);
        return dto;
    }


    /**
     * 获取当前在线用户人数
     */
    private Integer getOnlineUserCount() {
        String TOKEN_PREFIX = "user:token:"; // 你项目里的token前缀
        Set<String> keys = redisTemplate.keys(TOKEN_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }
}

