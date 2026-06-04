package com.inventory.modules.dashboard.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.dashboard.dto.DashboardIndexVO;
import com.inventory.modules.dashboard.service.DashboardIndexService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardIndexController {

    // 注入 Service
    @Resource
    private DashboardIndexService dashboardIndexService;

    /**
     * 首页聚合数据接口
     * period: 7d / 30d / 90d
     */
    @GetMapping("/index")
    public Result index(@RequestParam(defaultValue = "7d") String period) {
        // 调用 Service 获取数据
        DashboardIndexVO data = dashboardIndexService.getDashboardIndexData(period);
        // 返回给前端
        return Result.success(data);
    }
}