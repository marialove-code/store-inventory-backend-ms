package com.inventory.modules.order.orderinfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.dashboard.vo.CategoryPercentVO;
import com.inventory.modules.dashboard.vo.HotGoodsVO;
import com.inventory.modules.dashboard.vo.SalesTrendVO;
import com.inventory.modules.order.orderinfo.entity.OrderInfo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
* @author 95349
* @description 针对表【order_info(订单信息表)】的数据库操作Mapper
* @createDate 2026-05-31 11:09:26
* @Entity com.inventory.entity.OrderInfo
*/
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {



    // 今日销售额
    BigDecimal getTodaySalesAmount();

    // 今日有效订单数
    Integer getTodayOrderCount();


    // 按时间段获取销售趋势
    List<SalesTrendVO> getSalesTrend(@Param("days") int days);

    // 热销商品 TOP5
    List<HotGoodsVO>  getHotGoodsTop5();

    /**
     * 商品分类销售额占比
     */
    List<CategoryPercentVO> getCategoryPercent();

}




