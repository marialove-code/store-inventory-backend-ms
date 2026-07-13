package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 补货建议单条（对齐前端 AiReplenishItem）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReplenishItemVO {

    private String id;
    private String productName;
    private Integer currentStock;
    private Integer suggestQty;
    /** high | medium | low */
    private String riskLevel;
    private String reason;
}
