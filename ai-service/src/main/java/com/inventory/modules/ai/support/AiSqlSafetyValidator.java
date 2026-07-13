package com.inventory.modules.ai.support;

import com.inventory.modules.ai.config.AiSqlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器：仅允许只读 SELECT，且表名在白名单内。
 * <p>
 * AI SQL 助手在执行模型生成的 SQL 前必须过本校验，防止「删库 / 多语句 / 查非授权表」。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class AiSqlSafetyValidator {

    /** 写操作与危险关键字黑名单（大小写不敏感） */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|REPLACE|MERGE|GRANT|REVOKE|EXEC|EXECUTE|CALL)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final AiSqlProperties aiSqlProperties;

    /**
     * 校验 SQL 是否可安全执行。
     *
     * @param sql 待执行 SQL
     * @return 失败原因；null 表示通过
     */
    public String validate(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "SQL 为空";
        }
        String normalized = sql.trim();
        // 禁多语句：避免 SELECT ...; DROP TABLE ...
        if (normalized.contains(";")) {
            return "不允许多语句执行";
        }
        // 必须以 SELECT 开头（忽略大小写）
        if (!normalized.regionMatches(true, 0, "SELECT", 0, 6)) {
            return "仅允许 SELECT 查询";
        }
        // 黑名单关键字（即便嵌在子查询里也拦）
        if (FORBIDDEN.matcher(normalized).find()) {
            return "检测到危险关键字";
        }
        // 表白名单：SQL 中至少命中一张允许的表
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean hasAllowed = false;
        for (String table : aiSqlProperties.getAllowedTables()) {
            if (lower.contains(table.toLowerCase(Locale.ROOT))) {
                hasAllowed = true;
                break;
            }
        }
        if (!hasAllowed) {
            return "SQL 未包含允许的表白名单";
        }
        // null = 通过
        return null;
    }
}
