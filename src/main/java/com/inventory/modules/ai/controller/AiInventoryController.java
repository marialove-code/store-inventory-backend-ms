package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.modules.ai.service.AiInventoryService;
import com.inventory.modules.ai.vo.AiReplenishItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 库存补货建议 HTTP 接口。
 * <p>路由：GET /api/ai/inventory/replenish</p>
 */
@Tag(name = "AI 库存补货")
@RestController
@RequestMapping("/ai/inventory")
@RequiredArgsConstructor
public class AiInventoryController {

    private final AiInventoryService aiInventoryService;

    @Operation(summary = "低库存商品 AI 补货建议")
    @RateLimit(limit = 20, period = 60, msg = "补货建议请求过于频繁，请稍后再试")
    @GetMapping("/replenish")
    public Result<List<AiReplenishItemVO>> replenish() {
        return Result.success(aiInventoryService.replenishAdvice());
    }
}
