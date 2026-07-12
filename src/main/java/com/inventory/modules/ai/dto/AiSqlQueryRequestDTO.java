package com.inventory.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiSqlQueryRequestDTO {

    @NotBlank(message = "查询内容不能为空")
    @Size(max = 500, message = "查询内容不能超过 500 字")
    private String query;
}
