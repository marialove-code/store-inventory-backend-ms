package com.inventory.modules.invertory.stockwarn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预警页「只改阈值」请求体。
 * <p>
 * 与库存列表页的 {@link com.inventory.modules.invertory.stock.dto.StockWarnDTO}
 * （含 stock + stockWarn）不同，本 DTO 仅含预警阈值。
 * </p>
 */
@Data
public class StockWarnDTO {

    @NotNull(message = "预警阈值不能为空")
    private Integer stockWarn;
}
