package com.inventory.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.ai.constant.AiReplenishPrompts;
import com.inventory.modules.ai.service.AiInventoryService;
import com.inventory.modules.ai.support.AiLlmSupport;
import com.inventory.modules.ai.vo.AiReplenishItemVO;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AI 库存补货建议实现。
 * <p>
 * <b>是什么：</b>「查真实低库存 → 规则算建议量/风险 → 可选 LLM 润色原因」的混合方案。
 * </p>
 * <p>
 * <b>怎么用：</b>{@code GET /api/ai/inventory/replenish}，前端预警页表格展示。
 * </p>
 * <p>
 * <b>常见问题：</b>
 * <ul>
 *   <li>模型编造库存数字 → 数字来自 DB，LLM 只改 reason</li>
 *   <li>无预警商品 → 返回空列表，前端可提示「暂无补货建议」</li>
 *   <li>Key 未配置 → 仍返回规则版 reason</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInventoryServiceImpl implements AiInventoryService {

    /** 单次最多返回条数，避免一次拉太多低库存商品 */
    private static final int MAX_ITEMS = 20;

    private final InventoryStockMapper stockMapper;
    private final AiLlmSupport aiLlmSupport;
    private final ObjectMapper objectMapper;

    /**
     * 对外入口：生成补货建议列表。
     */
    @Override
    public List<AiReplenishItemVO> replenishAdvice() {
        // 1. 查数据库：库存 < 预警阈值（与 InventoryWarnService 条件一致）
        List<InventoryStock> warnList = stockMapper.selectList(
                Wrappers.<InventoryStock>lambdaQuery()
                        .apply("stock < stock_warn")
                        .orderByAsc(InventoryStock::getStock)
                        .last("LIMIT " + MAX_ITEMS)
        );
        // 无预警商品 → 空列表，前端展示「暂无」
        if (warnList.isEmpty()) {
            return List.of();
        }

        // 2. 规则层：先算出建议采购量、风险等级（不依赖模型，保证数字可信）
        List<AiReplenishItemVO> base = new ArrayList<>(warnList.size());
        for (InventoryStock stock : warnList) {
            base.add(buildRuleItem(stock));
        }

        // 3. 分支 A：无 Key → 直接返回规则结果，不调通义
        if (!aiLlmSupport.isApiKeyConfigured()) {
            return base;
        }
        try {
            // 4. 分支 B：把规则结果 JSON 当上下文，让模型只润色 reason / riskLevel
            String context = objectMapper.writeValueAsString(base);
            String raw = aiLlmSupport.callText(AiReplenishPrompts.SYSTEM, context);
            JsonNode arr = aiLlmSupport.parseJsonTree(raw);
            if (arr != null && arr.isArray() && !arr.isEmpty()) {
                // 合并：保留 DB 数字，覆盖文案类字段
                return mergeLlmResult(base, arr);
            }
        } catch (Exception ex) {
            // 5. 分支 C：LLM 失败 → 仍用规则结果，不抛 500
            log.warn("补货建议 LLM 增强失败，使用规则结果", ex);
        }
        return base;
    }

    /**
     * 单条规则建议：库存数字、建议量、风险、默认原因文案。
     */
    private AiReplenishItemVO buildRuleItem(InventoryStock stock) {
        // 当前库存；空则按 0
        int current = stock.getStock() != null ? stock.getStock() : 0;
        // 预警线；非法则默认 10
        int warn = stock.getStockWarn() != null && stock.getStockWarn() > 0 ? stock.getStockWarn() : 10;
        // 建议量：补到约 2 倍预警线，且至少为 warn
        int suggest = Math.max(warn * 2 - current, warn);
        // 风险：远低于预警 → high；低于预警 → medium；否则 low
        String risk;
        if (current <= warn / 2) {
            risk = "high";
        } else if (current < warn) {
            risk = "medium";
        } else {
            risk = "low";
        }
        String reason = String.format("当前库存 %d，低于预警线 %d，建议补货 %d 件以恢复安全水位。", current, warn, suggest);
        return AiReplenishItemVO.builder()
                .id(String.valueOf(stock.getId()))
                .productName(stock.getGoodsName())
                .currentStock(current)
                .suggestQty(suggest)
                .riskLevel(risk)
                .reason(reason)
                .build();
    }

    /**
     * 把 LLM 返回的数组「贴回」规则结果：只改文案，不改数字。
     * <p>
     * 入参：
     * <ul>
     *   <li>{@code base}：规则算好的列表（含真实 currentStock / suggestQty）</li>
     *   <li>{@code arr}：模型返回的 JSON 数组，期望与 base 一一对应</li>
     * </ul>
     * 为什么按索引合并：Prompt 要求模型按同样顺序返回，用下标对齐最简单；
     * 不按商品 id 匹配，避免模型漏 id / 写错 id。
     * </p>
     */
    private List<AiReplenishItemVO> mergeLlmResult(List<AiReplenishItemVO> base, JsonNode arr) {
        List<AiReplenishItemVO> merged = new ArrayList<>(base.size());
        // 按下标遍历：第 i 条规则结果 ↔ 模型数组第 i 个对象
        for (int i = 0; i < base.size(); i++) {
            AiReplenishItemVO item = base.get(i);
            // 模型条数可能少于 base（偷懒漏返回）→ 该下标无节点，保留规则原文
            JsonNode node = i < arr.size() ? arr.get(i) : null;
            if (node != null && node.isObject()) {
                // 只读文案字段；库存数字、建议量故意不读，防止模型乱改
                String reason = aiLlmSupport.textOrNull(node.path("reason"));
                String risk = aiLlmSupport.textOrNull(node.path("riskLevel"));
                // 有非空文案才覆盖；空串/null 不冲掉规则 reason
                if (StringUtils.hasText(reason)) {
                    item.setReason(reason);
                }
                if (StringUtils.hasText(risk)) {
                    item.setRiskLevel(risk);
                }
            }
            merged.add(item);
        }
        // 合并后再按风险排序：high → medium → low，运营优先看高风险
        merged.sort(Comparator.comparing((AiReplenishItemVO o) -> riskOrder(o.getRiskLevel())));
        return merged;
    }

    /** 风险排序权重：数字越小越靠前 */
    private int riskOrder(String risk) {
        if ("high".equalsIgnoreCase(risk)) {
            return 0;
        }
        if ("medium".equalsIgnoreCase(risk)) {
            return 1;
        }
        return 2;
    }
}
