package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.ai.dto.AiProductSearchRequestDTO;
import com.inventory.modules.ai.service.AiProductSearchService;
import com.inventory.modules.ai.vo.AiProductParseVO;
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
@RestController
@RequestMapping("/ai/product")
@RequiredArgsConstructor
@Validated
public class AiProductSearchController {

    private final AiProductSearchService aiProductSearchService;
    @PostMapping("/parse")
    public Result<AiProductParseVO> parse(@Valid @RequestBody AiProductSearchRequestDTO dto) {
        return Result.success(aiProductSearchService.parse(dto));
    }
}
