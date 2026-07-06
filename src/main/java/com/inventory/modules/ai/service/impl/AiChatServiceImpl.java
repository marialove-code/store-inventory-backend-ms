package com.inventory.modules.ai.service.impl;

import com.inventory.modules.ai.config.AiChatProperties;
import com.inventory.modules.ai.constant.AiChatPrompts;
import com.inventory.modules.ai.dto.AiChatRequestDTO;
import com.inventory.modules.ai.service.AiChatService;
import com.inventory.modules.ai.support.ChatSessionStore;
import com.inventory.modules.ai.vo.AiChatResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI 智能客服核心业务实现类（第三站核心）。
 * <p>
 * 职责：接收 Controller 转来的用户问题，拼 Prompt、调通义千问、维护会话、返回回复。
 * 不负责：HTTP 鉴权（第二站 Controller + JWT）、参数校验（Controller + DTO）。
 * </p>
 * <p>
 * 生活类比整条链路：
 * <ol>
 *   <li>用户发 message → 顾客开口问「怎么查库存」</li>
 *   <li>{@link ChatSessionStore#getHistory} → 店员翻小本子看刚才聊到哪</li>
 *   <li>拼 System + 历史 + 本轮 User → 《岗位手册》+ 聊天记录 + 新问题</li>
 *   <li>{@link ChatModel#call} → 外包呼叫中心（通义）回话</li>
 *   <li>{@link ChatSessionStore#appendTurn} → 把小本子更新，下次还能接上</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    /**
     * Spring AI 注入的对话模型客户端（第一站配置连通义千问）。
     * 业务只调 {@link ChatModel#call(Prompt)}，不手写 HTTP。
     */
    private final ChatModel chatModel;

    /**
     * 会话存储：按 sessionId 读写多轮历史（User/Assistant 消息对）。
     * 当前实现为内存 Map，详见 {@link com.inventory.modules.ai.support.InMemoryChatSessionStore}。
     */
    private final ChatSessionStore chatSessionStore;

    /**
     * 业务侧 AI 配置：历史条数上限、降级话术等（inventory.ai.chat.*）。
     */
    private final AiChatProperties aiChatProperties;

    /**
     * 通义 API Key，与 application.yml 中 spring.ai.openai.api-key 同源。
     * <p>
     * 这里单独读一遍是为了：Key 未配置时<strong>不调用</strong>模型，直接降级，
     * 避免 chatModel.call 抛错或产生无意义请求。
     * </p>
     */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * 处理一轮智能客服对话（对外唯一业务入口）。
     * <p>
     * 调用链：Controller {@code chat(dto)} → 本方法 → {@link #callModel} → 通义。
     * </p>
     *
     * @param dto 含用户本轮 message、可选 sessionId（多轮时前端原样带回）
     * @return sessionId + 助手回复 reply，封装为 {@link AiChatResponseVO}
     */
    @Override
    public AiChatResponseVO chat(AiChatRequestDTO dto) {
        // 1. 确定本场对话的会话 ID（新用户发号，老用户沿用）
        String sessionId = resolveSessionId(dto.getSessionId());

        // 2. 去掉首尾空格，避免无意义 Token 消耗
        String userMessage = dto.getMessage().trim();

        // 3. 分支 A：Key 未配置 → 不调模型，直接返回 yml 里的 fallback-reply（优雅降级）
        if (!StringUtils.hasText(apiKey)) {
            log.warn("DASHSCOPE_API_KEY 未配置，AI 客服返回降级话术 sessionId={}", sessionId);
            return buildResponse(sessionId, aiChatProperties.getFallbackReply());
        }

        try {
            // 4. 分支 B 正常路径：拼 Prompt → 调通义 → 拿到自然语言回复
            String reply = callModel(sessionId, userMessage);

            // 5. 把「用户这句 + 助手答句」写入会话存储，供下一轮 getHistory 使用
            //    注意：在 callModel 成功之后才写入，失败则不污染历史
            chatSessionStore.appendTurn(sessionId, userMessage, reply);

            // 6. 返回给前端：sessionId 务必带回，reply 展示在悬浮窗
            return buildResponse(sessionId, reply);
        } catch (Exception ex) {
            // 7. 分支 C：网络超时、DashScope 限流、5xx 等 → 用户仍看到友好话术，后台留 ERROR 日志
            log.error("AI 客服调用失败 sessionId={} message={}", sessionId, abbreviate(userMessage), ex);
            return buildResponse(sessionId, aiChatProperties.getFallbackReply());
        }
    }

    /**
     * 组装 Prompt 并调用大模型（本类最核心的方法）。
     * <p>
     * <strong>大模型本身无状态</strong>：每次 HTTP 请求互不记忆；「多轮」靠我们把 history 塞进 messages。
     * </p>
     * <p>
     * Prompt 消息顺序（通义按此顺序理解上下文）：
     * <ol>
     *   <li>{@link SystemMessage} — 系统提示词，相当于《岗位手册》</li>
     *   <li>历史 {@link Message} — 此前若干轮 User/Assistant</li>
     *   <li>{@link UserMessage} — 用户<strong>本轮</strong>输入</li>
     * </ol>
     * </p>
     *
     * @param sessionId   会话 ID，用于从 Store 取历史
     * @param userMessage 用户本轮问题（已 trim）
     * @return 模型生成的文本；空回复时走 fallback
     */
    private String callModel(String sessionId, String userMessage) {
        List<Message> messages = new ArrayList<>();

        // ① 系统提示词：固定进销存客服角色、模块范围、防幻觉要求（见 AiChatPrompts）
        messages.add(new SystemMessage(AiChatPrompts.CUSTOMER_SERVICE_SYSTEM));

        // ② 多轮上下文：不含 System，仅为历史 User/Assistant 对（首次对话通常为空列表）
        messages.addAll(chatSessionStore.getHistory(sessionId));

        // ③ 本轮用户问题（必须放在最后，模型优先响应当前问句）
        messages.add(new UserMessage(userMessage));

        // ④ 一次性发给 ChatModel；底层由 Spring AI 按 OpenAI 兼容协议请求 DashScope
        ChatResponse response = chatModel.call(new Prompt(messages));

        // ⑤ 从响应中取出助手文本（Spring AI 1.0 M6 标准写法：getResult → getOutput → getText）
        String reply = response.getResult().getOutput().getText();

        // ⑥ 模型返回空串时，仍走业务降级，避免前端空白
        if (!StringUtils.hasText(reply)) {
            return aiChatProperties.getFallbackReply();
        }

        return reply.trim();
    }

    /**
     * 解析或生成会话 ID。
     * <p>
     * 生活类比：老顾客报会员号（sessionId），新顾客现场办一张（UUID）。
     * </p>
     *
     * @param sessionId 前端传入，可为 null/空（首次对话）
     * @return 非空 sessionId，32 位十六进制（无横线的 UUID）
     */
    private String resolveSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId.trim();
        }
        // 首次对话：生成新 ID，响应里返回给前端，后续请求原样带回
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 组装统一响应体，供 Controller 包一层 Result.success 返回。
     *
     * @param sessionId 本场会话 ID（前端下一轮必须带回）
     * @param reply     展示给用户的助手文案（可能是通义生成，也可能是 fallback）
     */
    private AiChatResponseVO buildResponse(String sessionId, String reply) {
        return AiChatResponseVO.builder()
                .sessionId(sessionId)
                .reply(reply)
                .build();
    }

    /**
     * 日志专用：截断过长用户输入。
     * <p>
     * 异常日志里若打印完整 message，可能被恶意超长文本刷屏；只保留前 80 字便于排查。
     * </p>
     */
    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }
}
