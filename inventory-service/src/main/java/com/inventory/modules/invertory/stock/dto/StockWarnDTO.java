package com.inventory.modules.invertory.stock.dto;

import lombok.Data;

/**
 * 修改库存预警 / 可用库存的请求体。
 * <p>对应前端「编辑库存预警」表单；字段与单体保持一致。</p>
 */
@Data
public class StockWarnDTO {

    /** 可用库存 */
    private Integer stock;

    /** 预警阈值 */
    private Integer stockWarn;
}
