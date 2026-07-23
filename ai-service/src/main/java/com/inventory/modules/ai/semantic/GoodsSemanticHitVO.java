package com.inventory.modules.ai.semantic;

import lombok.Builder;
import lombok.Data;

/**
 * 智搜 V2 语义检索命中条目。
 */
@Data
@Builder
public class GoodsSemanticHitVO {

    /** 商品主键 */
    private Long goodsId;

    /** 入库时的原文块（名+规格+品牌+分类） */
    private String chunkText;

    /**
     * 相似度分数（由余弦距离换算：1 - distance，越大越像）。
     */
    private Double score;
}
