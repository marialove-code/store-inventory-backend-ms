package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI SQL 查询结果（对齐前端 AiSqlParseResult）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSqlParseVO {

    private String sql;
    private List<AiSqlColumnVO> columns;
    private List<java.util.Map<String, Object>> rows;
}
