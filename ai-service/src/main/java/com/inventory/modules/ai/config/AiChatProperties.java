package com.inventory.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 客服业务配置项。
 * <p>
 * 生活类比：大模型像「外包呼叫中心」，{@link #fallbackReply} 是电话占线时的自动语音；
 * {@link #maxHistoryMessages} 像店员能记住的最近几轮对话，太长会占满「工作台记忆」且多耗 Token 费用。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "inventory.ai.chat")
public class AiChatProperties {

    /**
     * 单个会话在内存中保留的最大消息条数（仅统计用户/助手轮次，不含 System）。
     * 超出后丢弃最早的消息，避免 Prompt 过长。
     */
    private int maxHistoryMessages = 20;

    /**
     * 模型调用失败或 Key 未配置时的兜底回复（用户仍能看到友好提示）。
     */
    private String fallbackReply =
            "抱歉，智能客服暂时繁忙，请稍后再试。您也可以点击上方快捷问题，或联系管理员。";

    /**
     * 会话存储实现：memory（JVM 内存，默认）| redis（Redis + TTL，学习/生产可选）。
     * <p>
     * 切换为 redis 时，需存在 {@link com.inventory.modules.ai.support.RedisChatSessionStore} Bean；
     * memory 时使用 {@link com.inventory.modules.ai.support.InMemoryChatSessionStore}。
     * </p>
     */
    private String sessionStore = "memory";

    /**
     * Redis 会话 Key 的 TTL（秒）。仅在 session-store=redis 时生效。
     * 默认 604800 = 7 天无活动后自动删除，防止垃圾 session 占内存。
     */
    private long sessionTtlSeconds = 604800L;
}
