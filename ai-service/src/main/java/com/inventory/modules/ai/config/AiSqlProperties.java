package com.inventory.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI SQL 助手安全配置。
 * <p>
 * Text-to-SQL 必须限制表白名单与只读 SELECT，防止模型生成危险语句。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "inventory.ai.sql")
public class AiSqlProperties {

    /**
     * 单次查询最大返回行数，防止一次拉全表 OOM。
     */
    private int maxRows = 100;

    /**
     * 允许出现在 SQL 中的表名（小写，与 PostgreSQL 实际表名一致）。
     */
    private List<String> allowedTables = new ArrayList<>(List.of(
            "goods_product",
            "goods_category",
            "goods_brand",
            "inventory_stock",
            "inventory_flow",
            "inventory_in",
            "inventory_out",
            "order_info",
            "order_delivery",
            "order_refund",
            "sys_user",
            "sys_role",
            "sys_operation_log"
    ));
}
