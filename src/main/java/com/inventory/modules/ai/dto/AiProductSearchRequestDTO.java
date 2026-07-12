package com.inventory.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 商品智搜请求体（V1：自然语言 → 结构化筛选条件）。
 */
@Data
public class AiProductSearchRequestDTO {

    /**
     * 用户输入的自然语言搜索描述。
     */
    @NotBlank(message = "搜索内容不能为空")
    @Size(max = 500, message = "搜索内容不能超过 500 字")
    private String query;
}
