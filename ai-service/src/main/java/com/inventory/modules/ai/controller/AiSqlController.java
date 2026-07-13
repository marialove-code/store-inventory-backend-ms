package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.ai.dto.AiSqlQueryRequestDTO;
import com.inventory.modules.ai.service.AiSqlService;
import com.inventory.modules.ai.vo.AiSqlParseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI SQL 助手 HTTP 接口（Text-to-SQL + 只读执行）。
 * <p>路由：POST /api/ai/sql/query</p>
 */
@RestController
@RequestMapping("/ai/sql")
@RequiredArgsConstructor
@Validated
public class AiSqlController {

    private final AiSqlService aiSqlService;
    @PostMapping("/query")
    public Result<AiSqlParseVO> query(@Valid @RequestBody AiSqlQueryRequestDTO dto) {
        return Result.success(aiSqlService.query(dto));
    }
}
