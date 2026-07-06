package com.inventory.modules.ai.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Redis 会话历史中的单条消息（可序列化 DTO）。
 * <p>
 * Spring AI 的 {@link org.springframework.ai.chat.messages.Message} 接口不便直接进 Redis，
 * 因此用简单 POJO 存 role + content，读写时再转回 UserMessage / AssistantMessage。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色：user 或 assistant（不含 system，System 每次由 Service 现拼） */
    private String role;

    /** 消息正文 */
    private String content;

    public static ChatHistoryEntry user(String text) {
        return new ChatHistoryEntry("user", text);
    }

    public static ChatHistoryEntry assistant(String text) {
        return new ChatHistoryEntry("assistant", text);
    }
}
