package com.inventory.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.ai.constant.AiProductSearchPrompts;
import com.inventory.modules.ai.dto.AiProductSearchRequestDTO;
import com.inventory.modules.ai.service.AiProductSearchService;
import com.inventory.modules.ai.vo.AiProductParseVO;
import com.inventory.modules.ai.vo.AiProductSearchFiltersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 商品智搜 V1 核心业务实现（自然语言 → 结构化筛选条件）。
 * <p>
 * <b>智搜 V1 做什么：</b>用户说「查询已下架的鼠标」，本类调通义千问把意图解析成 JSON，
 * 前端拿到 {@link AiProductParseVO#getFilters()} 后写入商品列表的 keyword、shelfStatus 等，
 * 仍走<strong>现有商品查询接口</strong>，不改数据库层。
 * </p>
 * <p>
 * <b>和 {@link AiChatServiceImpl} 客服的区别：</b>
 * <ul>
 *   <li>无 sessionId、无 {@link com.inventory.modules.ai.support.ChatSessionStore} 多轮历史</li>
 *   <li>Prompt 要求模型输出 <strong>JSON</strong>（结构化输出），不是自然语言 reply</li>
 *   <li>单次调用：仅 System + User 两条消息</li>
 * </ul>
 * </p>
 * <p>
 * <b>和智搜 V2（RAG）的区别：</b>V1 不做向量检索；V2 才用 Embedding + 向量库做语义搜商品/文档。
 * </p>
 * <p>
 * 生活类比：客服是「店员口头回答」；智搜 V1 是「把顾客的话翻译成一张<strong>筛选工单</strong>」
 * （keyword=鼠标、状态=下架），交给后台系统按工单查列表。
 * </p>
 * <p>
 * <b>调用链：</b>
 * {@link com.inventory.modules.ai.controller.AiProductSearchController#parse}
 * → 本类 {@link #parse} → {@link ChatModel#call} → 解析 JSON → 返回 VO
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProductSearchServiceImpl implements AiProductSearchService {

    /**
     * 匹配模型返回中的 markdown 代码块：{@code ```json ... ```}。
     * <p>
     * Prompt 虽要求「只输出 JSON」，模型仍可能包一层代码块，需先剥掉再 Jackson 解析。
     * </p>
     */
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 规则降级时识别库存条件，如「库存低于50」「库存小于 50」。
     * 匹配到的数字用于生成 {@link AiProductParseVO#getStockHint()} 展示文案。
     */
    private static final Pattern STOCK_HINT = Pattern.compile("库存(?:低于|小于|不足)\\s*(\\d+)");

    /**
     * Spring AI 对话模型（与客服共用同一 Bean，第一站配置连通义 qwen-turbo）。
     */
    private final ChatModel chatModel;

    /**
     * Jackson 解析模型返回的 JSON 字符串为 {@link AiProductParseVO} 各字段。
     */
    private final ObjectMapper objectMapper;

    /**
     * 通义 API Key，与 {@code spring.ai.openai.api-key} 同源。
     * <p>
     * 未配置时不调用模型，直接走 {@link #ruleBasedFallback}，保证本地开发/演示可用。
     * </p>
     */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * 解析用户自然语言搜索为结构化筛选条件（对外唯一业务入口）。
     * <p>
     * <b>主流程：</b>
     * <ol>
     *   <li>trim 用户 query</li>
     *   <li>apiKey 空 → 规则降级（不调通义）</li>
     *   <li>拼 Prompt：System（解析器角色 + JSON 规范）+ User（用户原话）</li>
     *   <li>{@link ChatModel#call} → 取文本 → {@link #parseModelJson}</li>
     *   <li>解析成功 → {@link #normalize} 补全空字段；失败或异常 → 规则降级</li>
     * </ol>
     * </p>
     * <p>
     * <b>降级路径（三层，保证可用性）：</b>
     * <ul>
     *   <li>Key 未配置</li>
     *   <li>模型调用抛异常（超时、限流等）</li>
     *   <li>返回文本无法解析为合法 JSON</li>
     * </ul>
     * 均回落到 {@link #ruleBasedFallback}，逻辑对齐前端原 Mock {@code parseAiProductQuery}。
     * </p>
     *
     * @param dto 含 {@link AiProductSearchRequestDTO#getQuery()}，Controller 层已 @Valid 校验非空
     * @return insight（展示用）+ filters（前端写搜索条件）+ stockHint（可选提示）
     */
    @Override
    public AiProductParseVO parse(AiProductSearchRequestDTO dto) {
        String query = dto.getQuery().trim();

        // 分支 A：Key 未配置 → 不调模型，规则解析（开发环境友好）
        if (!StringUtils.hasText(apiKey)) {
            log.warn("DASHSCOPE_API_KEY 未配置，商品智搜走规则降级 query={}", abbreviate(query));
            return ruleBasedFallback(query);
        }

        try {
            // 分支 B：正常路径 — 仅 System + User，无历史（与客服 callModel 不同）
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(AiProductSearchPrompts.PRODUCT_SEARCH_SYSTEM),
                    new UserMessage(query)
            )));

            // 从响应取出模型原始文本（应为 JSON，见 AiProductSearchPrompts 约束）
            String text = response.getResult().getOutput().getText();
            // 文本 → VO；成功则 normalize 补空，失败则落到分支 C 后的规则降级
            AiProductParseVO parsed = parseModelJson(text);
            if (parsed != null) {
                return normalize(parsed, query);
            }
            log.warn("商品智搜 JSON 解析失败，走规则降级 query={} raw={}", abbreviate(query), abbreviate(text));
        } catch (Exception ex) {
            // 分支 C：网络/通义异常 → 规则降级，不向上抛 500（与客服 fallback 思路一致）
            log.error("商品智搜调用失败 query={}", abbreviate(query), ex);
        }
        // 分支 B 解析失败 或 分支 C 异常，统一规则兜底
        return ruleBasedFallback(query);
    }

    /**
     * 将模型返回的文本解析为 {@link AiProductParseVO}。
     * <p>
     * 步骤：{@link #extractJson} 提纯 JSON 串 → {@link ObjectMapper#readTree} → 逐字段读取。
     * 使用 {@link JsonNode#path(String)} 避免字段缺失时抛异常。
     * </p>
     * <p>
     * 期望 JSON 结构（与 Prompt 示例一致）：
     * <pre>
     * {
     *   "insight": "AI 已理解您的需求：…",
     *   "filters": { "keyword", "shelfStatus", "sortField", "sortOrder" },
     *   "stockHint": "可选"
     * }
     * </pre>
     * </p>
     *
     * @param raw 模型原始输出，可能含 markdown 或前后废话
     * @return 解析成功返回 VO；空串或 JSON 非法时返回 null（由调用方走降级）
     */
    private AiProductParseVO parseModelJson(String raw) {
        // 空串直接失败 → 外层走规则降级
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        // 先提纯：去掉 ```json 或前后废话，只留 {...}
        String json = extractJson(raw.trim());
        try {
            // 字符串 → JsonNode 树，方便 path 安全取值（缺字段不抛异常）
            JsonNode root = objectMapper.readTree(json);
            // 从 filters 子节点拼出筛选条件对象
            AiProductSearchFiltersVO filters = AiProductSearchFiltersVO.builder()
                    .keyword(textOrNull(root.path("filters").path("keyword")))
                    .shelfStatus(intOrNull(root.path("filters").path("shelfStatus")))
                    .sortField(textOrNull(root.path("filters").path("sortField")))
                    .sortOrder(textOrNull(root.path("filters").path("sortOrder")))
                    .build();
            // 组装对外 VO：展示文案 + 筛选条件 + 可选库存提示
            return AiProductParseVO.builder()
                    .insight(textOrNull(root.path("insight")))
                    .filters(filters)
                    .stockHint(textOrNull(root.path("stockHint")))
                    .build();
        } catch (Exception ex) {
            // JSON 非法 → 返回 null，由 parse() 走 ruleBasedFallback
            log.debug("解析模型 JSON 失败: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 从模型原始文本中提取 JSON 子串。
     * <p>
     * 兼容两种常见「不听话」输出：
     * <ol>
     *   <li>包裹在 {@code ```json ... ```} 代码块中 → 用 {@link #JSON_BLOCK} 提取</li>
     *   <li>JSON 前后有说明文字 → 取第一个 {@code {} } 之间的内容</li>
     * </ol>
     * 若都不匹配，原样返回 raw，交给 Jackson 尝试（可能失败触发降级）。
     * </p>
     *
     * @param raw 模型完整输出
     * @return 尽量纯净的 JSON 字符串
     */
    private String extractJson(String raw) {
        // 情况1：模型包了 markdown 代码块 → 取出中间内容
        Matcher matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 情况2：前后有说明文字 → 截取第一个 { 到最后一个 }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        // 情况3：都不像 JSON → 原样返回，交给 Jackson 试，失败再降级
        return raw;
    }

    /**
     * 解析成功后的兜底补全，避免返回「全空 filters」导致前端无法搜索。
     * <p>
     * <ul>
     *   <li>若 keyword、shelfStatus、sortField 全空 → 用用户原 query 前 32 字作 keyword</li>
     *   <li>若 insight 空 → 填默认友好文案</li>
     * </ul>
     * </p>
     *
     * @param vo    模型解析结果
     * @param query 用户原始输入（用于 keyword 兜底）
     * @return 补全后的 VO
     */
    private AiProductParseVO normalize(AiProductParseVO vo, String query) {
        // filters 可能为 null，先保证有对象可写
        AiProductSearchFiltersVO filters = vo.getFilters() != null ? vo.getFilters() : new AiProductSearchFiltersVO();
        // 三个筛选项全空 → 用用户原话当 keyword，避免前端「搜了个寂寞」
        if (!StringUtils.hasText(filters.getKeyword())
                && filters.getShelfStatus() == null
                && !StringUtils.hasText(filters.getSortField())) {
            filters.setKeyword(query.length() > 32 ? query.substring(0, 32) : query);
        }
        // insight 空 → 补一句默认展示文案
        if (!StringUtils.hasText(vo.getInsight())) {
            vo.setInsight("AI 已理解您的需求，正在应用筛选条件…");
        }
        vo.setFilters(filters);
        return vo;
    }

    /**
     * 规则降级：不依赖大模型，用关键词/正则从 query 推断筛选条件。
     * <p>
     * <b>触发场景：</b>Key 未配置、模型调用失败、JSON 解析失败。
     * </p>
     * <p>
     * <b>规则说明（与前端 Mock {@code parseAiProductQuery} 对齐）：</b>
     * <ul>
     *   <li>含「下架」→ shelfStatus=0；含「上架」→ shelfStatus=1</li>
     *   <li>含预设商品词（鼠标、键盘等）→ keyword</li>
     *   <li>匹配库存正则 → 仅生成 stockHint（列表 API 可能不支持按库存筛）</li>
     *   <li>含销量/最近/高 → sortField=salePrice, sortOrder=desc（演示用售价代销量）</li>
     *   <li>以上皆无 → 整句 query 截断作 keyword</li>
     * </ul>
     * </p>
     * <p>
     * insight 字段拼接各识别项，供前端灰框展示，用户体验与真 AI 路径一致。
     * </p>
     *
     * @param query 用户自然语言搜索
     * @return 始终非 null 的解析结果
     */
    private AiProductParseVO ruleBasedFallback(String query) {
        // parts：拼 insight 展示用；filters：真正给前端搜列表
        List<String> parts = new ArrayList<>();
        AiProductSearchFiltersVO filters = new AiProductSearchFiltersVO();

        // 规则1：上架状态关键词
        if (query.contains("下架")) {
            filters.setShelfStatus(0);
            parts.add("上架状态 = 已下架");
        } else if (query.contains("上架")) {
            filters.setShelfStatus(1);
            parts.add("上架状态 = 已上架");
        }

        // 规则2：演示词表命中 → keyword
        String keyword = extractKeyword(query);
        if (keyword != null) {
            filters.setKeyword(keyword);
            parts.add("关键词 = " + keyword);
        }

        // 规则3：库存低于 N → 只写 stockHint（列表接口可能筛不了库存）
        String stockHint = null;
        Matcher stockMatcher = STOCK_HINT.matcher(query);
        if (stockMatcher.find()) {
            stockHint = "库存 < " + stockMatcher.group(1) + "（列表接口暂不支持按库存筛，请在结果中核对库存列）";
            parts.add(stockHint);
        }

        // 规则4：销量相关 → 演示用售价降序代替真实销量排序
        if (query.contains("销量") && (query.contains("高") || query.contains("最近"))) {
            filters.setSortField("salePrice");
            filters.setSortOrder("desc");
            parts.add("排序 = 按售价降序（演示）");
        }

        // 规则5：上面都没命中 → 整句当 keyword，保证至少能搜
        if (parts.isEmpty()) {
            filters.setKeyword(query.length() > 32 ? query.substring(0, 32) : query);
            parts.add("关键词 = " + filters.getKeyword());
        }

        // 拼 insight；若因无 Key 降级，文案前加提示，方便联调辨认
        String insight = "AI 已理解您的需求：" + String.join("；", parts) + "。正在应用筛选条件…";
        if (!StringUtils.hasText(apiKey)) {
            insight = "（模型未配置，已使用规则解析）" + insight;
        }

        return AiProductParseVO.builder()
                .insight(insight)
                .filters(filters)
                .stockHint(stockHint)
                .build();
    }

    /**
     * 规则降级用的简易商品词表匹配。
     * <p>
     * 仅覆盖演示常见品类；生产可改为从分类/品牌表加载，或完全依赖模型解析。
     * </p>
     *
     * @param query 用户输入
     * @return 命中的关键词，未命中返回 null
     */
    private String extractKeyword(String query) {
        // 演示用固定词表；生产可改为读分类/品牌表，或完全依赖 LLM
        String[] words = {"鼠标", "键盘", "机油", "滤芯", "轮胎", "液压", "大灯"};
        for (String word : words) {
            if (query.contains(word)) {
                return word;
            }
        }
        return null;
    }

    /**
     * 从 JSON 节点安全读取字符串；null、缺失、"null" 字符串均视为无值。
     *
     * @param node Jackson 节点
     * @return 非空 trim 后的文本，否则 null
     */
    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText(null);
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 从 JSON 节点安全读取整数（用于 shelfStatus：0 下架 / 1 上架）。
     * <p>
     * 兼容模型输出数字或字符串形式的 "0"/"1"。
     * </p>
     *
     * @param node Jackson 节点
     * @return 整数值，无法解析时 null
     */
    private Integer intOrNull(JsonNode node) {
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

    /**
     * 日志专用：截断过长 query/模型输出，避免刷屏。
     *
     * @param text 原始文本
     * @return 最多 80 字 + 省略号
     */
    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }
}
