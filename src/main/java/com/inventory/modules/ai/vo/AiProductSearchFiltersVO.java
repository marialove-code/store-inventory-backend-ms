package com.inventory.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 解析出的商品列表筛选条件（与前端 ProductSearchParams 可对齐的字段子集）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProductSearchFiltersVO {

    /** 商品名称/编码关键词 */
    private String keyword;

    /**
     * 上架状态：1=已上架，0=已下架；null 表示不限制。
     */
    private Integer shelfStatus;

    /** 排序字段，如 salePrice、createTime、stock */
    private String sortField;

    /** 排序方向：asc 或 desc */
    private String sortOrder;
}
