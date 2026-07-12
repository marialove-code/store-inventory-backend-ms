package com.inventory.modules.ai.service;

import com.inventory.modules.ai.vo.AiInsightSummaryVO;
import com.inventory.modules.ai.vo.AiSalesForecastPointVO;

import java.util.List;

/**
 * AI 看板增强：洞察摘要 + 销售预测。
 * <p>
 * <b>是否连库：</b>是。复用 {@link com.inventory.modules.dashboard.service.DashboardIndexService} 聚合数据。
 * </p>
 */
public interface AiDashboardService {

    AiInsightSummaryVO insight(String period);

    List<AiSalesForecastPointVO> salesForecast(String period);
}
