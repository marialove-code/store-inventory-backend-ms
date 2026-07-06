package com.inventory.modules.ai.service;

import com.inventory.modules.ai.dto.AiChatRequestDTO;
import com.inventory.modules.ai.vo.AiChatResponseVO;

/**
 * AI 智能客服服务接口。
 */
public interface AiChatService {

    /**
     * 处理一轮用户提问，返回模型回复并维护会话上下文。
     *
     * @param dto 用户消息与可选 sessionId
     * @return 会话 ID + 助手回复
     */
    AiChatResponseVO chat(AiChatRequestDTO dto);
}
