package com.inventory.modules.invertory.stockwarn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存预警阈值调整DTO
 * 用于接收前端传入的预警阈值参数
 */
@Data
public class StockWarnDTO {

    /**
     * 库存预警阈值
     * 当商品库存小于此值时，标记为库存预警
     */
    @NotNull(message = "预警阈值不能为空")
    private Integer stockWarn;
}