package com.inventory.modules.invertory.stock.dto;

import lombok.Data;

@Data
public class StockWarnDTO {


    /** 可用库存 */
    private Integer stock;


    /** 预警阈值 */
    private Integer stockWarn;


}