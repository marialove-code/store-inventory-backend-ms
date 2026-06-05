package com.inventory.modules.shop.dashboard.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.shop.records.service.ShopSaleRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门店看板接口
 */
@RestController
@RequestMapping("/shop/dashboard")
@RequiredArgsConstructor
public class ShopDashboardController {

    private final ShopSaleRecordService shopSaleRecordService;

    /**
     * 看板
     */
    @GetMapping("/overview")
    public Result<?> getDashboard(@RequestParam(required = false) String year) {
        return shopSaleRecordService.getShopDashboardInfo(year);
    }
}