package com.inventory.modules.invertory.stockin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增入库单请求体。
 * <p>对应接口：{@code POST /inventory/stockin}</p>
 */
@Data
public class StockInAddDTO {

    @NotBlank(message = "商品ID不能为空")
    private String goodsId;

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    @NotNull(message = "入库数量不能为空")
    @Positive(message = "入库数量必须大于0")
    private Integer receiptQty;

    @Size(max = 200, message = "备注信息不能超过200个字符")
    private String remark;
}
