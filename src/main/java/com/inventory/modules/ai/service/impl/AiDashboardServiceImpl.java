package com.inventory.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inventory.modules.ai.constant.AiDashboardPrompts;
import com.inventory.modules.ai.service.AiDashboardService;
import com.inventory.modules.ai.support.AiLlmSupport;
import com.inventory.modules.ai.vo.AiInsightSummaryVO;
import com.inventory.modules.ai.vo.AiSalesForecastPointVO;
import com.inventory.modules.dashboard.dto.DashboardIndexVO;
import com.inventory.modules.dashboard.service.DashboardIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 看板增强实现：洞察摘要 + 7 日销售预测。
 * <p>
 * <b>是什么：</b>在已有 {@link DashboardIndexService} 聚合数据之上，用 LLM 生成可读洞察与趋势预测；
 * 数字指标以 DB 统计为准，模型只负责「解读」与「预测曲线」。
 * </p>
 * <p>
 * <b>怎么用：</b>
 * {@code GET /api/ai/dashboard/insight?period=7d}、
 * {@code GET /api/ai/dashboard/sales-forecast?period=7d}。
 * </p>
 * <p>
 * <b>问题与解决：</b>
 * <ul>
 *   <li>预测不可信 → 标注为 AI 参考，基于历史序列外推</li>
 *   <li>period 非 7d → 洞察仍可用全量 stats；预测默认取最近 7 个点</li>
 *   <li>无 Key → 规则算 healthScore + 线性外推预测</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDashboardServiceImpl implements AiDashboardService {

    /** 预测横轴展示标签（演示用，非真实日期） */
    private static final String[] FORECAST_LABELS = {
            "预测一", "预测二", "预测三", "预测四", "预测五", "预测六", "预测七"
    };

    private final DashboardIndexService dashboardIndexService;
    private final AiLlmSupport aiLlmSupport;
    private final ObjectMapper objectMapper;

    /**
     * 看板洞察：真实 stats → 规则 healthScore → 可选 LLM 润色 suggestion。
     */
    @Override
    public AiInsightSummaryVO insight(String period) {
        String p = StringUtils.hasText(period) ? period : "7d";
        // 1. 复用看板已有聚合（今日销量、预警数等），不重复写 SQL
        DashboardIndexVO data = dashboardIndexService.getDashboardIndexData(p);
        AiInsightSummaryVO rule = buildRuleInsight(data);

        // 分支 A：无 Key → 规则洞察
        if (!aiLlmSupport.isApiKeyConfigured()) {
            return rule;
        }
        try {
            // 分支 B：只把摘要字段喂给模型，让它写 suggestion / 微调分数
            String context = objectMapper.writeValueAsString(summarizeForLlm(data));
            String raw = aiLlmSupport.callText(AiDashboardPrompts.INSIGHT_SYSTEM, context);
            JsonNode root = aiLlmSupport.parseJsonTree(raw);
            if (root != null && root.isObject()) {
                return mergeInsight(rule, root);
            }
        } catch (Exception ex) {
            // 分支 C：失败仍返回规则结果
            log.warn("看板洞察 LLM 失败，使用规则结果", ex);
        }
        return rule;
    }

    /**
     * 销售预测：历史 trend → 规则线性外推 → 可选 LLM 生成 7 点曲线。
     */
    @Override
    public List<AiSalesForecastPointVO> salesForecast(String period) {
        String p = StringUtils.hasText(period) ? period : "7d";
        DashboardIndexVO data = dashboardIndexService.getDashboardIndexData(p);
        List<DashboardIndexVO.SalesTrendVO> trend = data.getSalesTrend() != null ? data.getSalesTrend() : List.of();
        List<AiSalesForecastPointVO> rule = ruleForecast(trend);

        // 分支 A：无 Key 或无历史点 → 规则外推
        if (!aiLlmSupport.isApiKeyConfigured() || trend.isEmpty()) {
            return rule;
        }
        try {
            // 分支 B：把历史序列交给模型，要求返回至少 7 个预测点
            String context = objectMapper.writeValueAsString(trend);
            String raw = aiLlmSupport.callText(AiDashboardPrompts.FORECAST_SYSTEM, context);
            JsonNode arr = aiLlmSupport.parseJsonTree(raw);
            if (arr != null && arr.isArray() && arr.size() >= 7) {
                return parseForecastArray(arr);
            }
        } catch (Exception ex) {
            log.warn("销售预测 LLM 失败，使用规则外推", ex);
        }
        return rule;
    }

    /** 压缩看板数据，控制 Prompt token（只留洞察需要的字段） */
    private ObjectNode summarizeForLlm(DashboardIndexVO data) {
        ObjectNode node = objectMapper.createObjectNode();
        if (data.getStats() != null) {
            node.put("todaySales", data.getStats().getTodaySales());
            node.put("todayOrders", data.getStats().getTodayOrders());
            node.put("totalStock", data.getStats().getTotalStock());
            node.put("warnStockCount", data.getStats().getWarnStockCount());
        }
        if (data.getHotTop5() != null) {
            node.set("hotTop5", objectMapper.valueToTree(data.getHotTop5()));
        }
        if (data.getMonitor() != null) {
            node.put("redisStatus", data.getMonitor().getRedisStatus());
            node.put("onlineUsers", data.getMonitor().getOnlineUserCount());
        }
        return node;
    }

    /**
     * 规则洞察：按预警数量估健康分 + 补货建议文案。
     */
    private AiInsightSummaryVO buildRuleInsight(DashboardIndexVO data) {
        int warn = data.getStats() != null && data.getStats().getWarnStockCount() != null
                ? data.getStats().getWarnStockCount() : 0;
        int totalStock = data.getStats() != null && data.getStats().getTotalStock() != null
                ? data.getStats().getTotalStock() : 1;
        // 基准 90，每预警扣 2，夹在 40~95
        int health = Math.max(40, Math.min(95, 90 - warn * 2));
        // 预警占比过高再扣 10
        if (totalStock > 0 && warn * 100 / totalStock > 5) {
            health -= 10;
        }
        String suggestion = warn > 0
                ? String.format("当前有 %d 个商品低于安全库存，建议优先补货并关注热销品类。", warn)
                : "库存结构整体健康，可维持现有补货节奏，关注销售趋势变化。";
        return AiInsightSummaryVO.builder()
                .healthScore(health)
                .replenishCount(warn)
                .slowMovingCount(Math.max(warn / 2, 0))
                .suggestion(suggestion)
                .analysisPath("/stock/warn")
                .build();
    }

    /** 合并 LLM：有值才覆盖规则字段 */
    private AiInsightSummaryVO mergeInsight(AiInsightSummaryVO rule, JsonNode root) {
        Integer score = aiLlmSupport.intOrNull(root.path("healthScore"));
        Integer replenish = aiLlmSupport.intOrNull(root.path("replenishCount"));
        Integer slow = aiLlmSupport.intOrNull(root.path("slowMovingCount"));
        String suggestion = aiLlmSupport.textOrNull(root.path("suggestion"));
        String path = aiLlmSupport.textOrNull(root.path("analysisPath"));
        if (score != null) {
            rule.setHealthScore(score);
        }
        if (replenish != null) {
            rule.setReplenishCount(replenish);
        }
        if (slow != null) {
            rule.setSlowMovingCount(slow);
        }
        if (StringUtils.hasText(suggestion)) {
            rule.setSuggestion(suggestion);
        }
        if (StringUtils.hasText(path)) {
            rule.setAnalysisPath(path);
        }
        return rule;
    }

    /** 无模型时：以最近一天金额为基，按约 2%/点 线性外推 7 天 */
    private List<AiSalesForecastPointVO> ruleForecast(List<DashboardIndexVO.SalesTrendVO> trend) {
        BigDecimal avg = averageAmount(trend);
        List<AiSalesForecastPointVO> list = new ArrayList<>(7);
        BigDecimal last = trend.isEmpty() ? avg : trend.get(trend.size() - 1).getAmount();
        if (last == null) {
            last = avg;
        }
        for (int i = 0; i < 7; i++) {
            BigDecimal factor = BigDecimal.valueOf(1.0 + (i + 1) * 0.02);
            BigDecimal amount = last.multiply(factor).setScale(0, RoundingMode.HALF_UP);
            list.add(AiSalesForecastPointVO.builder()
                    .date(FORECAST_LABELS[i])
                    .amount(amount)
                    .build());
        }
        return list;
    }

    private BigDecimal averageAmount(List<DashboardIndexVO.SalesTrendVO> trend) {
        if (trend.isEmpty()) {
            return BigDecimal.valueOf(10000);
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (DashboardIndexVO.SalesTrendVO p : trend) {
            if (p.getAmount() != null) {
                sum = sum.add(p.getAmount());
                count++;
            }
        }
        if (count == 0) {
            return BigDecimal.valueOf(10000);
        }
        return sum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    /** 取模型数组前 7 点；date/amount 缺失时用默认标签与 0 */
    private List<AiSalesForecastPointVO> parseForecastArray(JsonNode arr) {
        List<AiSalesForecastPointVO> list = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            JsonNode node = arr.get(i);
            String date = aiLlmSupport.textOrNull(node.path("date"));
            if (!StringUtils.hasText(date)) {
                date = FORECAST_LABELS[i];
            }
            BigDecimal amount = null;
            JsonNode amountNode = node.path("amount");
            if (amountNode.isNumber()) {
                amount = amountNode.decimalValue();
            } else if (amountNode.isTextual()) {
                try {
                    amount = new BigDecimal(amountNode.asText().trim());
                } catch (NumberFormatException ignored) {
                    amount = BigDecimal.ZERO;
                }
            }
            list.add(AiSalesForecastPointVO.builder().date(date).amount(amount).build());
        }
        return list;
    }
}
