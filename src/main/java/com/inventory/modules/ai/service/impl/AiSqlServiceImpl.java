package com.inventory.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.inventory.modules.ai.config.AiSqlProperties;
import com.inventory.modules.ai.constant.AiSqlPrompts;
import com.inventory.modules.ai.dto.AiSqlQueryRequestDTO;
import com.inventory.modules.ai.service.AiSqlService;
import com.inventory.modules.ai.support.AiLlmSupport;
import com.inventory.modules.ai.support.AiSqlSafetyValidator;
import com.inventory.modules.ai.vo.AiSqlColumnVO;
import com.inventory.modules.ai.vo.AiSqlParseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI SQL 助手实现：自然语言 → SELECT → 安全校验 → 执行 → 表格数据。
 * <p>
 * <b>为什么用：</b>报表类问题频繁但运营不会写 SQL；LLM 生成 + 白名单校验兼顾灵活与安全。
 * </p>
 * <p>
 * <b>怎么用：</b>{@code POST /api/ai/sql/query}，body {@code { "query": "最近7天销量TOP10" }}。
 * </p>
 * <p>
 * <b>问题与解决：</b>
 * <ul>
 *   <li>SQL 注入 / 删库 → 仅 SELECT + 表白名单 + 禁多语句</li>
 *   <li>一次查太多 → maxRows 限制 + 强制 LIMIT</li>
 *   <li>模型 SQL 错误 → 规则模板降级（常见三问）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSqlServiceImpl implements AiSqlService {

    private final AiLlmSupport aiLlmSupport;
    private final AiSqlSafetyValidator sqlSafetyValidator;
    private final AiSqlProperties aiSqlProperties;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 对外入口：自然语言问数 → 返回 SQL + 列定义 + 行数据。
     */
    @Override
    public AiSqlParseVO query(AiSqlQueryRequestDTO dto) {
        // 小写副本专供规则匹配（含「分类」「今日」等关键词）
        String q = dto.getQuery().trim().toLowerCase(Locale.ROOT);

        // 分支 A：有 Key → 先让模型生成 { sql, columns }
        if (aiLlmSupport.isApiKeyConfigured()) {
            String raw = aiLlmSupport.callText(AiSqlPrompts.SYSTEM, dto.getQuery().trim());
            JsonNode root = aiLlmSupport.parseJsonTree(raw);
            if (root != null) {
                String sql = aiLlmSupport.textOrNull(root.path("sql"));
                // 校验通过且执行成功才返回；失败则落到规则模板
                AiSqlParseVO executed = tryExecute(sql, root);
                if (executed != null) {
                    return executed;
                }
            }
        }

        // 分支 B：无 Key / 模型失败 / SQL 非法 / 执行报错 → 规则模板兜底
        return ruleFallback(q);
    }

    /**
     * 安全校验 + 限行 + 执行；任一步失败返回 null，由外层降级。
     */
    private AiSqlParseVO tryExecute(String sql, JsonNode root) {
        if (!StringUtils.hasText(sql)) {
            return null;
        }
        // 仅 SELECT、禁危险关键字、表白名单
        String err = sqlSafetyValidator.validate(sql);
        if (err != null) {
            log.warn("SQL 安全校验未通过: {} sql={}", err, sql);
            return null;
        }
        // 模型忘写 LIMIT 时强制加上，防止一次扫全表
        String limitedSql = ensureLimit(sql);
        try {
            // 按 ResultSet 元数据动态拼行 Map（列名 → 值）
            List<Map<String, Object>> rows = jdbcTemplate.query(limitedSql, (rs, rowNum) -> {
                ResultSetMetaData meta = rs.getMetaData();
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String label = meta.getColumnLabel(i);
                    row.put(label, rs.getObject(i));
                }
                return row;
            });
            // 双保险：即便 LIMIT 失效，也截断到配置上限
            if (rows.size() > aiSqlProperties.getMaxRows()) {
                rows = rows.subList(0, aiSqlProperties.getMaxRows());
            }
            // 列定义优先用模型给的中文 title；否则用结果集第一行的 key
            List<AiSqlColumnVO> columns = buildColumns(root, rows);
            return AiSqlParseVO.builder().sql(limitedSql).columns(columns).rows(rows).build();
        } catch (Exception ex) {
            log.warn("SQL 执行失败 sql={}", limitedSql, ex);
            return null;
        }
    }

    /** 无 LIMIT 则追加，避免大结果集拖垮接口 */
    private String ensureLimit(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" limit ")) {
            return sql;
        }
        return sql + " LIMIT " + aiSqlProperties.getMaxRows();
    }

    /**
     * 组装前端表格列：优先模型 columns；否则从首行 Map 的 key 推断。
     */
    private List<AiSqlColumnVO> buildColumns(JsonNode root, List<Map<String, Object>> rows) {
        JsonNode cols = root.path("columns");
        if (cols.isArray() && !cols.isEmpty()) {
            List<AiSqlColumnVO> list = new ArrayList<>();
            cols.forEach(c -> list.add(AiSqlColumnVO.builder()
                    .title(aiLlmSupport.textOrNull(c.path("title")))
                    .dataIndex(aiLlmSupport.textOrNull(c.path("dataIndex")))
                    .key(aiLlmSupport.textOrNull(c.path("key")))
                    .build()));
            return list;
        }
        if (rows.isEmpty()) {
            return List.of();
        }
        List<AiSqlColumnVO> list = new ArrayList<>();
        for (String key : rows.get(0).keySet()) {
            list.add(AiSqlColumnVO.builder().title(key).dataIndex(key).key(key).build());
        }
        return list;
    }

    /**
     * 规则降级：按关键词匹配固定三套报表 SQL（对齐原前端 mock 场景）。
     */
    private AiSqlParseVO ruleFallback(String q) {
        // 场景1：按分类汇总库存
        if (q.contains("分类") && q.contains("库存")) {
            return executeTemplate(
                    """
                            SELECT category_name AS category, SUM(stock) AS qty
                            FROM inventory_stock
                            GROUP BY category_name
                            ORDER BY qty DESC
                            LIMIT 20
                            """,
                    List.of(
                            col("分类", "category"),
                            col("库存总量", "qty")
                    )
            );
        }
        // 场景2：今日订单数 / 金额
        if (q.contains("今日") && (q.contains("订单") || q.contains("金额"))) {
            return executeTemplate(
                    """
                            SELECT TO_CHAR(create_time, 'YYYY-MM-DD') AS order_date,
                                   COUNT(*) AS order_count,
                                   COALESCE(SUM(order_amount), 0) AS total_amount
                            FROM order_info
                            WHERE DATE(create_time) = CURRENT_DATE
                            GROUP BY DATE(create_time), TO_CHAR(create_time, 'YYYY-MM-DD')
                            """,
                    List.of(
                            col("日期", "order_date"),
                            col("订单数", "order_count"),
                            col("金额", "total_amount")
                    )
            );
        }
        // 场景3（默认）：近 7 天销量 TOP10
        return executeTemplate(
                """
                        SELECT goods_name AS product_name,
                               SUM(buy_qty) AS total_qty,
                               COALESCE(SUM(order_amount), 0) AS total_amount
                        FROM order_info
                        WHERE create_time >= CURRENT_DATE - INTERVAL '7 days'
                        GROUP BY goods_name
                        ORDER BY total_qty DESC
                        LIMIT 10
                        """,
                List.of(
                        col("商品名称", "product_name"),
                        col("销量", "total_qty"),
                        col("销售额", "total_amount")
                )
        );
    }

    private AiSqlColumnVO col(String title, String dataIndex) {
        return AiSqlColumnVO.builder().title(title).dataIndex(dataIndex).key(dataIndex).build();
    }

    /** 执行手写模板 SQL；失败仍返回空 rows，便于前端展示「无数据」而非 500 */
    private AiSqlParseVO executeTemplate(String sql, List<AiSqlColumnVO> columns) {
        String trimmed = sql.trim();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.query(trimmed, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                // 只取模板声明的列，避免多余字段干扰前端 Table
                for (AiSqlColumnVO c : columns) {
                    row.put(c.getDataIndex(), rs.getObject(c.getDataIndex()));
                }
                return row;
            });
            return AiSqlParseVO.builder().sql(trimmed).columns(columns).rows(rows).build();
        } catch (Exception ex) {
            log.error("规则 SQL 执行失败", ex);
            return AiSqlParseVO.builder()
                    .sql(trimmed)
                    .columns(columns)
                    .rows(List.of())
                    .build();
        }
    }
}
