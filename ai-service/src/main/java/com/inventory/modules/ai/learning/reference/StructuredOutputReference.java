package com.inventory.modules.ai.learning.reference;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【学习用 · 非生产 Bean】LangChain「Structured Output / JsonOutputParser」↔ Spring AI 对照。
 * <p>
 * <b>LangChain 写法（概念）：</b>
 * <pre>
 * from langchain.output_parsers import JsonOutputParser
 * parser = JsonOutputParser(pydantic_object=SearchFilters)
 * chain = prompt | llm | parser
 * </pre>
 * </p>
 * <p>
 * <b>本项目等价：</b>
 * {@link com.inventory.modules.ai.service.impl.AiProductSearchServiceImpl#parse}
 * — 智搜 V1：自然语言 → JSON 筛选条件 → 前端写表格，仍查 PG。
 * </p>
 * <p>
 * <b>为什么不用 Spring AI 内置 StructuredOutputConverter？</b>
 * M6 版本各模型支持不一；我们采用「Prompt 约束 + 正则抠 JSON + Jackson」，与 LangChain 的 OutputParser 职责相同，更可控。
 * </p>
 */
public final class StructuredOutputReference {

    /** 与 {@link com.inventory.modules.ai.support.AiLlmSupport} 相同：剥 ```json 代码块 */
    private static final Pattern JSON_BLOCK =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private StructuredOutputReference() {
    }

    /**
     * LangChain 单轮 LLMChain：只有 System + User，无 Memory。
     */
    public static String invokeLlmChain(ChatModel chatModel, String systemPrompt, String userQuery) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userQuery)
        ));
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    /**
     * LangChain JsonOutputParser 的 Java 手写版：从模型「不听话」输出里提取 JSON。
     * <p>
     * 模型可能返回：纯 JSON / ```json ... ``` / 前后带解释文字 — 都要兼容。
     * </p>
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String trimmed = raw.trim();

        // 1) markdown 代码块
        Matcher m = JSON_BLOCK.matcher(trimmed);
        if (m.find()) {
            return m.group(1).trim();
        }
        // 2) 截取第一个 { ... }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * 降级策略（LangChain 教程常省略，面试要提）：
     * <ul>
     *   <li>Key 未配置 → 不调 LLM</li>
     *   <li>parse 失败 → 关键词规则 {@code ruleBasedFallback}</li>
     * </ul>
     * 见生产类 {@link com.inventory.modules.ai.service.impl.AiProductSearchServiceImpl#ruleBasedFallback}
     */
    public static String ruleBasedFallbackHint() {
        return "LangChain 链末端应始终考虑 fallback；本项目用 Java 规则保证商品页可用。";
    }
}
