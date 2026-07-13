package com.inventory.modules.ai.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.ai.dto.AiChatRequestDTO;
import com.inventory.modules.ai.service.AiChatService;
import com.inventory.modules.ai.vo.AiChatResponseVO;
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
 * 鉴权：微服务阶段暂不鉴权（后续由 Gateway / platform 统一）。
 * </p>
 * <p>
 * 生活类比：这是门店「咨询台」的窗口——顾客（已登录用户）递纸条提问，
 * 窗口转给后台客服系统（{@link AiChatService}），再把答复递回去。
 * </p>
 */
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
    @PostMapping("/chat")
    public Result<AiChatResponseVO> chat(@Valid @RequestBody AiChatRequestDTO dto) {
        return Result.success(aiChatService.chat(dto));
    }
}
