package com.inventory.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 客服对话请求体。
 * <p>
 * 生活类比：你去门店问导购——{@link #message} 是你当场问的话；
 * {@link #sessionId} 像会员号，让店员记得你刚才问过什么（多轮上下文）。
 * </p>
 */
@Data
public class AiChatRequestDTO {

    /**
     * 用户本轮输入的问题（必填）。
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过 2000 字")
    private String message;

    /**
     * 会话 ID（可选）。
     * <ul>
     *   <li>首次对话可不传，服务端会生成并返回</li>
     *   <li>后续请求带上同一 sessionId，即可记住上下文</li>
     * </ul>
     */
    @Size(max = 64, message = "sessionId 过长")
    private String sessionId;
}
