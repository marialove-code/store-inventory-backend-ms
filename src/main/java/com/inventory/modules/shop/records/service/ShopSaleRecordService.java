package com.inventory.modules.shop.records.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.response.Result;
import com.inventory.modules.shop.records.dto.ShopSaleCreateDto;
import com.inventory.modules.shop.records.entity.ShopSaleRecord;
import com.inventory.modules.shop.records.entity.ShopSaleRecordListParam;

public interface ShopSaleRecordService extends IService<ShopSaleRecord> {


    /**
     * 分页查询销售流水
     */
    Result<?> getSaleRecordPage(ShopSaleRecordListParam param);

    /**
     * 创建销售订单（开单）
     */
    Result<?> createSaleOrder(ShopSaleCreateDto dto);

    /**
     * 获取销售统计（今日+本月）
     */
    Result<?> getSaleStatistics();


    /**
     * 获取门店看板全部聚合数据
     */
    Result<?> getShopDashboardInfo(String year);
}