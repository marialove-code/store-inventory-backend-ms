package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.modules.ai.dto.AiSqlQueryRequestDTO;
import com.inventory.modules.ai.service.AiSqlService;
import com.inventory.modules.ai.vo.AiSqlParseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AI SQL 助手")
@RestController
@RequestMapping("/ai/sql")
@RequiredArgsConstructor
@Validated
public class AiSqlController {

    private final AiSqlService aiSqlService;

    @Operation(summary = "自然语言转 SQL 并执行只读查询")
    @RateLimit(limit = 15, period = 60, msg = "SQL 助手请求过于频繁，请稍后再试")
    @PostMapping("/query")
    public Result<AiSqlParseVO> query(@Valid @RequestBody AiSqlQueryRequestDTO dto) {
        return Result.success(aiSqlService.query(dto));
    }
}
