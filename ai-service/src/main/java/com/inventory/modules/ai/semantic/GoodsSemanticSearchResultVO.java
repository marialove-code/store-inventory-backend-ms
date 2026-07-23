package com.inventory.modules.ai.semantic;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 智搜 V2 完整结果：TopK 命中 + RAG 生成说明。
 */
@Data
@Builder
public class GoodsSemanticSearchResultVO {

    /** 向量检索命中列表（按相关度） */
    private List<GoodsSemanticHitVO> hits;

    /**
     * RAG 生成的推荐说明（仅依据 hits 原文；失败时为兜底文案）。
     */
    private String ragSummary;
}
