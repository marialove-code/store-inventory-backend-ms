package com.inventory.modules.ai.support;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 聊天会话存储接口。
 * <p>
 * 生活类比：门店收银台旁的小本子，记下「这位顾客刚才问了啥、你怎么答的」。
 * 当前实现放在 JVM 内存；重启会清空，生产可换 Redis。
 * </p>
 */
public interface ChatSessionStore {

    /**
     * 读取某会话的历史消息（不含 System，仅为用户/助手轮次）。
     */
    List<Message> getHistory(String sessionId);

    /**
     * 追加一轮对话并自动裁剪长度。
     *
     * @param sessionId  会话 ID
     * @param userText   用户说的话
     * @param assistantText 模型回复
     */
    void appendTurn(String sessionId, String userText, String assistantText);

    /**
     * 清空某会话（预留：用户点「新对话」时可调用）。
     */
    void clear(String sessionId);
}
