package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 看板 AI 洞察摘要（对齐前端 AiInsightSummary）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightSummaryVO {

    private Integer healthScore;
    private Integer replenishCount;
    private Integer slowMovingCount;
    private String suggestion;
    private String analysisPath;
}
