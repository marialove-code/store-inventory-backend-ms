package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.modules.ai.dto.AiChatRequestDTO;
import com.inventory.modules.ai.service.AiChatService;
import com.inventory.modules.ai.vo.AiChatResponseVO;
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
 * AI 智能客服 HTTP 接口。
 * <p>
 * 路由：POST /api/ai/chat（context-path=/api）
 * 鉴权：需登录（JWT），不在 Security 白名单内。
 * </p>
 * <p>
 * 生活类比：这是门店「咨询台」的窗口——顾客（已登录用户）递纸条提问，
 * 窗口转给后台客服系统（{@link AiChatService}），再把答复递回去。
 * </p>
 */
@Tag(name = "AI 智能客服")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Validated
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 发送一条客服消息（支持多轮，通过 sessionId 关联上下文）。
     * <p>
     * 限流：每用户 IP 每分钟最多 30 次，防止刷接口烧 Token。
     * </p>
     */
    @Operation(summary = "AI 客服对话")
    @RateLimit(limit = 30, period = 60, msg = "AI 客服请求过于频繁，请稍后再试")
    @PostMapping("/chat")
    public Result<AiChatResponseVO> chat(@Valid @RequestBody AiChatRequestDTO dto) {
        return Result.success(aiChatService.chat(dto));
    }
}
