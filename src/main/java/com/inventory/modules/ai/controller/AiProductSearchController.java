package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.modules.ai.dto.AiProductSearchRequestDTO;
import com.inventory.modules.ai.service.AiProductSearchService;
import com.inventory.modules.ai.vo.AiProductParseVO;
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
 * AI 商品智搜 HTTP 接口（V1）。
 * <p>
 * 路由：POST /api/ai/product/parse
 * </p>
 */
@Tag(name = "AI 商品智搜")
@RestController
@RequestMapping("/ai/product")
@RequiredArgsConstructor
@Validated
public class AiProductSearchController {

    private final AiProductSearchService aiProductSearchService;

    @Operation(summary = "自然语言解析为商品筛选条件")
    @RateLimit(limit = 30, period = 60, msg = "AI 智搜请求过于频繁，请稍后再试")
    @PostMapping("/parse")
    public Result<AiProductParseVO> parse(@Valid @RequestBody AiProductSearchRequestDTO dto) {
        return Result.success(aiProductSearchService.parse(dto));
    }
}
