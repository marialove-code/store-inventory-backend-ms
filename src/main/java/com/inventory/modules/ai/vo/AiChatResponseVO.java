package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 客服对话响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseVO {

    /**
     * 会话 ID，前端后续请求需原样带回。
     */
    private String sessionId;

    /**
     * 助手回复的文本内容。
     */
    private String reply;
}
