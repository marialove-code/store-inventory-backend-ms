package com.inventory.modules.order.orderinfo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建订单入参 DTO。
 */
@Data
public class OrderInfoDTO {

    @NotBlank(message = "下单用户名称不能为空")
    private String userName;

    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须大于0")
    private Long goodsId;

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    @NotNull(message = "成本价不能为空")
    @Min(value = 0, message = "成本价不能为负数")
    private BigDecimal costPrice;

    @NotNull(message = "售价不能为空")
    @Min(value = 0, message = "售价不能为负数")
    private BigDecimal salePrice;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer buyQty;

    /** 订单备注（可选） */
    private String remark;
}
