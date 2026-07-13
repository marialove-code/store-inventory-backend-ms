package com.inventory.modules.shop.records.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.shop.dashboard.vo.DashboardShopVO;
import com.inventory.modules.shop.dashboard.vo.ShopHotProductVO;
import com.inventory.modules.shop.records.entity.ShopSaleRecord;
import com.inventory.modules.shop.records.vo.ShopSaleStatsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShopSaleRecordMapper extends BaseMapper<ShopSaleRecord> {


    /**
     * 统计今日、本月销售总额
     */
    ShopSaleStatsVo selectSaleTotalStats();


    /**
     * 看板全量销售聚合：今日/月/季/年销售额、利润、订单、销量 + TOP5
     */
    DashboardShopVO selectDashboardSaleData(@Param("year") Integer year);


    /**
     * 热销Top5
     * @return
     */
    List<ShopHotProductVO> selectHotTop5Product(@Param("year") Integer year);
}