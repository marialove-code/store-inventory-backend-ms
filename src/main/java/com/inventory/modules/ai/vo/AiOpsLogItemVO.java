package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 运维日志分析条目（对齐前端 AiOpsLogItem）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiOpsLogItemVO {

    private String id;
    /** error | warn | info */
    private String level;
    private String service;
    private String message;
    private String time;
    private String analysis;
    private String solution;
    /** high | medium | low */
    private String risk;
}
