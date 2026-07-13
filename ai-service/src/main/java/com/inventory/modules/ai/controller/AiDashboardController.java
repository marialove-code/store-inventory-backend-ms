package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.ai.service.AiDashboardService;
import com.inventory.modules.ai.vo.AiInsightSummaryVO;
import com.inventory.modules.ai.vo.AiSalesForecastPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 看板增强 HTTP 接口：洞察摘要 + 销售预测。
 */
@RestController
@RequestMapping("/ai/dashboard")
@RequiredArgsConstructor
public class AiDashboardController {

    private final AiDashboardService aiDashboardService;
    @GetMapping("/insight")
    public Result<AiInsightSummaryVO> insight(
            @RequestParam(defaultValue = "7d") String period) {
        return Result.success(aiDashboardService.insight(period));
    }
    @GetMapping("/sales-forecast")
    public Result<List<AiSalesForecastPointVO>> salesForecast(
            @RequestParam(defaultValue = "7d") String period) {
        return Result.success(aiDashboardService.salesForecast(period));
    }
}
