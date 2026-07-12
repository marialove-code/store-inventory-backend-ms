package com.inventory.modules.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 模块共享 LLM 调用与 JSON 解析工具。
 * <p>
 * 各 AI 子功能（客服、智搜、补货、SQL、运维、看板）共用同一 {@link ChatModel}，
 * 本类抽取「拼 Prompt → call → 取文本 → 提纯 JSON」的重复逻辑，避免每个 Service 各写一遍。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiLlmSupport {

    /** 匹配 ```json ... ``` 代码块，便于从模型「不听话」输出里抠 JSON */
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * API Key 是否已配置（未配置时不应调用通义，应走各业务的规则降级）。
     */
    public boolean isApiKeyConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 单次对话：System + User，无多轮历史（智搜、SQL、补货等均适用）。
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户输入
     * @return 模型返回的纯文本；Key 未配置或调用失败时返回 empty
     */
    public String callText(String systemPrompt, String userMessage) {
        // 分支 A：无 Key → 空串，调用方自行降级
        if (!isApiKeyConfigured()) {
            return "";
        }
        try {
            // 仅 System + User，与智搜同构（客服多轮不走本方法）
            List<Message> messages = List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userMessage)
            );
            ChatResponse response = chatModel.call(new Prompt(messages));
            String text = response.getResult().getOutput().getText();
            return text != null ? text.trim() : "";
        } catch (Exception ex) {
            // 分支 B：调用失败 → 空串，不抛给业务层
            log.error("LLM 调用失败 user={}", abbreviate(userMessage), ex);
            return "";
        }
    }

    /**
     * 将模型输出解析为 JSON 树；失败返回 null。
     */
    public JsonNode parseJsonTree(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            // 先 extractJson 再 readTree，与智搜 parseModelJson 同思路
            return objectMapper.readTree(extractJson(raw.trim()));
        } catch (Exception ex) {
            log.debug("JSON 解析失败: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON 树反序列化为指定类型。
     */
    public <T> T readValue(JsonNode node, Class<T> type) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception ex) {
            log.debug("JSON 转对象失败 type={}: {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }

    /**
     * 读取 JSON 数组节点为 List。
     */
    public List<JsonNode> readArray(JsonNode root, String fieldName) {
        JsonNode arr = root.path(fieldName);
        if (!arr.isArray()) {
            return List.of();
        }
        List<JsonNode> list = new ArrayList<>(arr.size());
        arr.forEach(list::add);
        return list;
    }

    /** 安全读字符串；缺失/null 返回 null */
    public String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText(null);
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /** 安全读整数；兼容数字节点与字符串 "0"/"1" */
    public Integer intOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        String text = node.asText(null);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 日志截断，避免超长刷屏 */
    public String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }

    /**
     * 从模型原文提取 JSON 子串（对象或数组）。
     */
    private String extractJson(String raw) {
        // 1. markdown 代码块
        Matcher matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 2. 截取 {...}
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        // 3. 截取 [...]（补货/预测常返回数组）
        start = raw.indexOf('[');
        end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
