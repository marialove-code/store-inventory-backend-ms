package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.modules.ai.service.AiDashboardService;
import com.inventory.modules.ai.vo.AiInsightSummaryVO;
import com.inventory.modules.ai.vo.AiSalesForecastPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 看板增强 HTTP 接口：洞察摘要 + 销售预测。
 */
@Tag(name = "AI 看板增强")
@RestController
@RequestMapping("/ai/dashboard")
@RequiredArgsConstructor
public class AiDashboardController {

    private final AiDashboardService aiDashboardService;

    @Operation(summary = "看板 AI 运营洞察")
    @RateLimit(limit = 20, period = 60, msg = "看板洞察请求过于频繁，请稍后再试")
    @GetMapping("/insight")
    public Result<AiInsightSummaryVO> insight(
            @RequestParam(defaultValue = "7d") String period) {
        return Result.success(aiDashboardService.insight(period));
    }

    @Operation(summary = "未来 7 日销售趋势预测")
    @RateLimit(limit = 20, period = 60, msg = "销售预测请求过于频繁，请稍后再试")
    @GetMapping("/sales-forecast")
    public Result<List<AiSalesForecastPointVO>> salesForecast(
            @RequestParam(defaultValue = "7d") String period) {
        return Result.success(aiDashboardService.salesForecast(period));
    }
}
