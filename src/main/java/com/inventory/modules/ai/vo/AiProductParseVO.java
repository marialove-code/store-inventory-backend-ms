package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 商品智搜解析结果（V1 结构化输出）。
 * <p>
 * 与前端 {@code AiProductParseResult} 字段对齐，供商品列表页直接应用筛选。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProductParseVO {

    /** 展示给用户的理解说明，如「AI 已理解：关键词=鼠标…」 */
    private String insight;

    /** 可映射到商品列表查询的筛选条件 */
    private AiProductSearchFiltersVO filters;

    /**
     * 库存相关条件的提示（当前列表接口可能不支持按库存筛，仅展示）。
     */
    private String stockHint;
}
