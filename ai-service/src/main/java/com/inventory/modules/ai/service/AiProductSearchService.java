package com.inventory.modules.ai.service;

import com.inventory.modules.ai.dto.AiProductSearchRequestDTO;
import com.inventory.modules.ai.vo.AiProductParseVO;

/**
 * AI 商品智搜服务（V1：结构化输出，无 RAG）。
 */
public interface AiProductSearchService {

    /**
     * 将自然语言查询解析为商品筛选条件。
     *
     * @param dto 含 query 字段
     * @return insight + filters + stockHint
     */
    AiProductParseVO parse(AiProductSearchRequestDTO dto);
}
