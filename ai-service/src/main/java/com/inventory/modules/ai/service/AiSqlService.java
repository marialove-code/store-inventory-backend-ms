package com.inventory.modules.ai.service;

import com.inventory.modules.ai.dto.AiSqlQueryRequestDTO;
import com.inventory.modules.ai.vo.AiSqlParseVO;

/**
 * AI SQL 助手服务（Text-to-SQL + 只读执行）。
 * <p>
 * <b>为什么用：</b>运营/管理员用自然语言查报表，降低 SQL 门槛。
 * <b>是否连库：</b>是。校验通过后 {@link org.springframework.jdbc.core.JdbcTemplate} 执行 SELECT。
 * </p>
 */
public interface AiSqlService {

    AiSqlParseVO query(AiSqlQueryRequestDTO dto);
}
