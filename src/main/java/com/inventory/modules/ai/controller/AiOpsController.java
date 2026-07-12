package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.modules.ai.service.AiOpsService;
import com.inventory.modules.ai.vo.AiOpsLogItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 运维助手 HTTP 接口。
 * <p>路由：GET /api/ai/ops/analyze</p>
 */
@Tag(name = "AI 运维助手")
@RestController
@RequestMapping("/ai/ops")
@RequiredArgsConstructor
public class AiOpsController {

    private final AiOpsService aiOpsService;

    @Operation(summary = "分析最近系统操作日志")
    @RateLimit(limit = 20, period = 60, msg = "运维分析请求过于频繁，请稍后再试")
    @GetMapping("/analyze")
    public Result<List<AiOpsLogItemVO>> analyze()  {
        return Result.success(aiOpsService.analyzeRecent());
    }
}
