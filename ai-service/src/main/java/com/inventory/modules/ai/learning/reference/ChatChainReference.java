package com.inventory.modules.ai.learning.reference;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * 【学习用 · 非生产 Bean】LangChain「Chat + Memory」↔ Spring AI 对照。
 * <p>
 * <b>LangChain 写法（Python 概念）：</b>
 * <pre>
 * from langchain_openai import ChatOpenAI
 * from langchain.memory import ConversationBufferMemory
 * from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
 *
 * prompt = ChatPromptTemplate.from_messages([
 *   ("system", "你是进销存客服..."),
 *   MessagesPlaceholder("history"),
 *   ("human", "{input}")
 * ])
 * chain = prompt | llm
 * </pre>
 * </p>
 * <p>
 * <b>本项目等价写法：</b>
 * {@link com.inventory.modules.ai.service.impl.AiChatServiceImpl#callModel}
 * </p>
 * <p>
 * <b>对照表：</b>
 * <ul>
 *   <li>ChatOpenAI → {@link ChatModel}（yml 配通义 DashScope）</li>
 *   <li>MessagesPlaceholder("history") → {@code chatSessionStore.getHistory(sessionId)}</li>
 *   <li>system 模板 → {@link com.inventory.modules.ai.constant.AiChatPrompts#CUSTOMER_SERVICE_SYSTEM}</li>
 *   <li>LCEL {@code prompt | llm} → {@code chatModel.call(new Prompt(messages))}</li>
 * </ul>
 * </p>
 */
public final class ChatChainReference {

    private ChatChainReference() {
    }

    /**
     * 伪代码：展示一次「多轮客服」在 Spring AI 里如何拼 Prompt。
     * <p>
     * 注意：大模型<strong>无状态</strong>，Memory 是我们每次把 history 塞进 messages。
     * </p>
     *
     * @param chatModel     Spring 容器里的 ChatModel Bean（生产环境由 Spring AI 自动配置）
     * @param systemPrompt  相当于 LangChain 的 system 模板
     * @param history       相当于 ConversationBufferMemory 里之前的轮次
     * @param userInput     本轮用户问题
     * @return 助手回复文本
     */
    public static String invokeChatChain(
            ChatModel chatModel,
            String systemPrompt,
            List<Message> history,
            String userInput) {

        // Step 1：按顺序组装 messages（顺序很重要，模型按此理解上下文）
        List<Message> messages = new ArrayList<>();

        // LangChain: ("system", "...")
        messages.add(new SystemMessage(systemPrompt));

        // LangChain: MessagesPlaceholder("history") — 历史不含 system
        if (history != null) {
            messages.addAll(history);
        }

        // LangChain: ("human", "{input}")
        messages.add(new UserMessage(userInput));

        // Step 2：一次 RPC 调用（LangChain chain.invoke）
        ChatResponse response = chatModel.call(new Prompt(messages));

        // Step 3：取 assistant 文本（LangChain AIMessage.content）
        return response.getResult().getOutput().getText();
    }

    /**
     * 一轮对话结束后，应把 User + Assistant 追加进 Memory。
     * <p>
     * 生产代码：{@link com.inventory.modules.ai.support.ChatSessionStore#appendTurn}
     * </p>
     */
    public static List<Message> appendTurnToMemory(
            List<Message> existing,
            String userText,
            String assistantText) {

        List<Message> next = new ArrayList<>(existing == null ? List.of() : existing);
        next.add(new UserMessage(userText));
        next.add(new AssistantMessage(assistantText));
        return next;
    }
}
