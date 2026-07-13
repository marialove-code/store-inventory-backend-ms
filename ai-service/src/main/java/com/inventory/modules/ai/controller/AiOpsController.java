package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.ai.service.AiOpsService;
import com.inventory.modules.ai.vo.AiOpsLogItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 运维助手 HTTP 接口。
 * <p>路由：GET /api/ai/ops/analyze</p>
 */
@RestController
@RequestMapping("/ai/ops")
@RequiredArgsConstructor
public class AiOpsController {

    private final AiOpsService aiOpsService;
    @GetMapping("/analyze")
    public Result<List<AiOpsLogItemVO>> analyze()  {
        return Result.success(aiOpsService.analyzeRecent());
    }
}
