package com.inventory.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.modules.ai.constant.AiOpsPrompts;
import com.inventory.modules.ai.service.AiOpsService;
import com.inventory.modules.ai.support.AiLlmSupport;
import com.inventory.modules.ai.vo.AiOpsLogItemVO;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.inventory.modules.system.log.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 运维助手实现：读取真实操作日志 + LLM 分析。
 * <p>
 * <b>为什么用：</b>运维日志分散在表里，人工翻查慢；LLM 可批量归纳原因与处置建议。
 * </p>
 * <p>
 * <b>怎么用：</b>{@code GET /api/ai/ops/analyze}，前端左侧列表 + 右侧分析详情。
 * </p>
 * <p>
 * <b>问题与解决：</b>
 * <ul>
 *   <li>日志量大 → 只取最近 30 条</li>
 *   <li>模型幻觉 → 基于真实 requestUri、errorMessage，不编造不存在的 IP</li>
 *   <li>无 Key → 规则映射 level/risk + 模板 analysis</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiOpsServiceImpl implements AiOpsService {

    /** 单次分析条数上限，控制 Prompt 体积与接口耗时 */
    private static final int MAX_LOGS = 30;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperationLogService operationLogService;
    private final AiLlmSupport aiLlmSupport;
    private final ObjectMapper objectMapper;

    /**
     * 对外入口：最近操作日志 → 规则基线 → 可选 LLM 润色 analysis/solution。
     */
    @Override
    public List<AiOpsLogItemVO> analyzeRecent() {
        // 1. 查真实操作日志（按时间倒序，最多 30 条）
        List<SysOperationLog> logs = operationLogService.list(
                Wrappers.<SysOperationLog>lambdaQuery()
                        .orderByDesc(SysOperationLog::getCreateTime)
                        .last("LIMIT " + MAX_LOGS)
        );
        if (logs.isEmpty()) {
            return List.of();
        }

        // 2. 规则层：先定 level / risk / 模板分析（不依赖模型，保证有结果）
        List<AiOpsLogItemVO> base = logs.stream().map(this::toRuleItem).toList();

        // 分支 A：无 Key → 直接返回规则结果
        if (!aiLlmSupport.isApiKeyConfigured()) {
            return base;
        }
        try {
            // 分支 B：把规则结果当上下文，让模型润色 analysis / solution / risk / level
            String context = objectMapper.writeValueAsString(base);
            String raw = aiLlmSupport.callText(AiOpsPrompts.SYSTEM, context);
            JsonNode arr = aiLlmSupport.parseJsonTree(raw);
            if (arr != null && arr.isArray() && !arr.isEmpty()) {
                return mergeLlm(base, arr);
            }
        } catch (Exception ex) {
            // 分支 C：LLM 失败 → 仍用规则结果
            log.warn("运维日志 LLM 分析失败，使用规则结果", ex);
        }
        return base;
    }

    /** 单条日志 → 规则 VO（level / message / 模板分析与处置） */
    private AiOpsLogItemVO toRuleItem(SysOperationLog log) {
        String level = resolveLevel(log);
        String message = buildMessage(log);
        String service = resolveService(log);
        // 风险与 level 简单映射，供前端着色
        String risk = "error".equals(level) ? "high" : ("warn".equals(level) ? "medium" : "low");
        String analysis = ruleAnalysis(log, level);
        String solution = ruleSolution(log, level);
        String time = log.getCreateTime() != null ? log.getCreateTime().format(TIME_FMT) : "";
        return AiOpsLogItemVO.builder()
                .id(String.valueOf(log.getId()))
                .level(level)
                .service(service)
                .message(message)
                .time(time)
                .analysis(analysis)
                .solution(solution)
                .risk(risk)
                .build();
    }

    /** 规则定级：操作失败 → error；含超时/慢 → warn；否则 info */
    private String resolveLevel(SysOperationLog log) {
        if (log.getOperateStatus() != null && log.getOperateStatus() == 0) {
            return "error";
        }
        String err = log.getErrorMessage();
        if (StringUtils.hasText(err) && (err.contains("慢") || err.contains("超时") || err.toLowerCase(Locale.ROOT).contains("timeout"))) {
            return "warn";
        }
        return "info";
    }

    /** 展示文案：优先错误信息，否则 method + uri + title */
    private String buildMessage(SysOperationLog log) {
        if (StringUtils.hasText(log.getErrorMessage())) {
            return log.getErrorMessage();
        }
        return String.format("%s %s %s", log.getRequestMethod(), log.getRequestUri(), log.getTitle());
    }

    /** 服务名：优先操作标题，否则从 URI 第一段推断 */
    private String resolveService(SysOperationLog log) {
        if (StringUtils.hasText(log.getTitle())) {
            return log.getTitle();
        }
        if (StringUtils.hasText(log.getRequestUri())) {
            String uri = log.getRequestUri();
            int slash = uri.indexOf('/', 1);
            return slash > 0 ? uri.substring(1, slash) : uri;
        }
        return "System";
    }

    private String ruleAnalysis(SysOperationLog log, String level) {
        if ("error".equals(level)) {
            return String.format("操作「%s」执行失败，接口 %s，请结合错误信息与请求参数排查。", log.getTitle(), log.getRequestUri());
        }
        if ("warn".equals(level)) {
            return "接口响应偏慢或存在超时迹象，可能与数据库慢查询或下游依赖有关。";
        }
        return String.format("用户 %s 完成「%s」，属正常审计记录。", log.getUsername(), log.getTitle());
    }

    private String ruleSolution(SysOperationLog log, String level) {
        if ("error".equals(level)) {
            return "查看完整堆栈与 requestParams；复现后检查权限、参数校验与业务异常处理。";
        }
        if ("warn".equals(level)) {
            return "开启慢 SQL 日志，检查索引与分页；评估接口限流与缓存。";
        }
        return "无需处理，可作为合规审计留存。";
    }

    /**
     * 按索引合并：只覆盖文案类字段，保留 id/time/message 等真实日志字段。
     */
    private List<AiOpsLogItemVO> mergeLlm(List<AiOpsLogItemVO> base, JsonNode arr) {
        List<AiOpsLogItemVO> merged = new ArrayList<>(base.size());
        for (int i = 0; i < base.size(); i++) {
            AiOpsLogItemVO item = base.get(i);
            JsonNode node = i < arr.size() ? arr.get(i) : null;
            if (node != null && node.isObject()) {
                String analysis = aiLlmSupport.textOrNull(node.path("analysis"));
                String solution = aiLlmSupport.textOrNull(node.path("solution"));
                String risk = aiLlmSupport.textOrNull(node.path("risk"));
                String level = aiLlmSupport.textOrNull(node.path("level"));
                // 有值才覆盖，避免空串冲掉规则模板
                if (StringUtils.hasText(analysis)) {
                    item.setAnalysis(analysis);
                }
                if (StringUtils.hasText(solution)) {
                    item.setSolution(solution);
                }
                if (StringUtils.hasText(risk)) {
                    item.setRisk(risk);
                }
                if (StringUtils.hasText(level)) {
                    item.setLevel(level);
                }
            }
            merged.add(item);
        }
        return merged;
    }
}
