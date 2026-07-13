package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.ai.service.AiInventoryService;
import com.inventory.modules.ai.vo.AiReplenishItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 库存补货建议 HTTP 接口。
 * <p>路由：GET /api/ai/inventory/replenish</p>
 */
@RestController
@RequestMapping("/ai/inventory")
@RequiredArgsConstructor
public class AiInventoryController {

    private final AiInventoryService aiInventoryService;
    @GetMapping("/replenish")
    public Result<List<AiReplenishItemVO>> replenish() {
        return Result.success(aiInventoryService.replenishAdvice());
    }
}
