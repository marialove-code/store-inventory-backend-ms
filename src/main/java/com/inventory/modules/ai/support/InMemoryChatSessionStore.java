package com.inventory.modules.ai.support;

import com.inventory.modules.ai.config.AiChatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 JVM 内存的聊天会话存储实现（第四站核心）。
 * <p>
 * 实现 {@link ChatSessionStore} 接口，把多轮对话的 User/Assistant 消息按 sessionId 存在本机内存里，
 * 供 {@link com.inventory.modules.ai.service.impl.AiChatServiceImpl#callModel} 拼 Prompt 时读取历史。
 * </p>
 * <p>
 * 生活类比：收银台旁一排便签本——每个 sessionId 一本；每聊一轮记一行「问+答」；
 * 本子太厚就撕掉最早几页（trimHistory）；打烊（JVM 重启）本子全丢。
 * </p>
 * <p>
 * <strong>适用：</strong>本地开发、演示、求职项目。<br>
 * <strong>局限：</strong>重启丢失、多实例不共享、session 无 TTL 可能占满内存。<br>
 * <strong>生产升级：</strong>新增 {@code RedisChatSessionStore} 实现同一接口即可，Service 层不用改。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.ai.chat", name = "session-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryChatSessionStore implements ChatSessionStore {

    /**
     * 业务配置，主要用到 {@link AiChatProperties#getMaxHistoryMessages()} 控制单会话保留条数。
     */
    private final AiChatProperties aiChatProperties;

    /**
     * 会话仓库：sessionId → 该会话的历史消息列表。
     * <p>
     * 使用 {@link ConcurrentHashMap} 保证 <strong>不同 sessionId</strong> 之间并发读写安全；
     * 同一 sessionId 下的 List 在 {@link #appendTurn} 里再用 synchronized 保护，避免同一会话并发写乱序。
     * </p>
     * <p>
     * 注意：Map 本身 <strong>没有上限、没有 TTL</strong>，session 只增不减直到进程结束——
     * 生产环境应换 Redis 并设置过期时间，见 docs/AI功能复习手册.md「客服深挖 / ChatSessionStore」。
     * </p>
     */
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();

    /**
     * 读取指定会话的历史消息，供拼 Prompt 时使用。
     * <p>
     * 调用时机：{@link com.inventory.modules.ai.service.impl.AiChatServiceImpl#callModel}
     * 在添加本轮 {@link UserMessage} <strong>之前</strong> 调用。
     * </p>
     * <p>
     * 返回内容：
     * <ul>
     *   <li>仅含历史 {@link UserMessage} / {@link AssistantMessage}，<strong>不含 SystemMessage</strong></li>
     *   <li>System 每次由 Service 重新 {@code new SystemMessage(...)}，不存入 Store</li>
     *   <li>首次对话或重启后无记录 → 返回空列表</li>
     * </ul>
     * </p>
     * <p>
     * 为何 {@code new ArrayList<>(...)} 拷贝一份？
     * 避免调用方修改返回的 List 时误改 Store 内部数据（防御性拷贝）。
     * </p>
     *
     * @param sessionId 会话 ID，与前端/响应体中的 sessionId 一致
     * @return 历史消息副本，无记录时为空列表（非 null）
     */
    @Override
    public List<Message> getHistory(String sessionId) {
        return new ArrayList<>(sessionHistory.getOrDefault(sessionId, List.of()));
    }

    /**
     * 追加一轮完整对话（用户问 + 助手答），并自动裁剪超长历史。
     * <p>
     * 调用时机：{@link com.inventory.modules.ai.service.impl.AiChatServiceImpl#chat}
     * 在 {@code callModel} <strong>成功返回之后</strong> 才调用——失败或降级不会写入，避免污染历史。
     * </p>
     * <p>
     * 一轮对话在 List 里占 <strong>2 条</strong>：User → Assistant，顺序与真实聊天一致，
     * 这样下次 {@link #getHistory} 取出后直接 {@code messages.addAll(history)} 即可被模型理解。
     * </p>
     *
     * @param sessionId       会话 ID
     * @param userText        用户本轮输入（已 trim）
     * @param assistantText   模型本轮回复（或 fallback 文案——成功路径下一般为通义生成内容）
     */
    @Override
    public void appendTurn(String sessionId, String userText, String assistantText) {
        // computeIfAbsent：该 session 第一次聊天时创建空 List，相当于「新顾客领一本新便签」
        List<Message> history = sessionHistory.computeIfAbsent(sessionId, key -> new ArrayList<>());

        // 同一会话可能被快速连点触发并发，对 List 加锁保证 add 顺序正确
        synchronized (history) {
            history.add(new UserMessage(userText));
            history.add(new AssistantMessage(assistantText));
            // 写满后撕掉最早的消息，控制 Token 与内存
            trimHistory(history);
        }
    }

    /**
     * 清空指定会话的全部历史。
     * <p>
     * 当前前端未暴露「新对话」按钮，此方法为预留：用户主动开新话题时可 {@code clear} 后复用或换新 sessionId。
     * </p>
     *
     * @param sessionId 要清空的会话 ID；不存在则 no-op（remove 不抛异常）
     */
    @Override
    public void clear(String sessionId) {
        sessionHistory.remove(sessionId);
    }

    /**
     * 裁剪历史：只保留最近 {@link AiChatProperties#getMaxHistoryMessages()} 条消息。
     * <p>
     * 配置项 {@code inventory.ai.chat.max-history-messages}，默认 20。
     * 计数单位是 <strong>消息条数</strong>（User 和 Assistant 各算 1 条），不是「轮数」：
     * 例如 max=20 大约相当于最近 10 轮问答。
     * </p>
     * <p>
     * 策略：从列表头部 {@code remove(0)} 删除最早的，直到 size ≤ max。
     * 原因：模型更关心最近几轮；太早的对话对当前问题帮助小，且会浪费 Token 费用。
     * </p>
     *
     * @param history 当前会话的消息列表（调用方已持有锁）
     */
    private void trimHistory(List<Message> history) {
        int max = Math.max(2, aiChatProperties.getMaxHistoryMessages());
        while (history.size() > max) {
            history.remove(0);
        }
    }
}
