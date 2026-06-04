package com.inventory.modules.order.orderinfo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 新增订单入参DTO
 * 用于接收前端创建订单的请求参数，并做参数校验
 * @author 95349
 * @date 2026-05-31
 */
@Data
public class OrderInfoDTO {

    /**
     * 下单用户名称
     * 非空校验
     */
    @NotBlank(message = "下单用户名称不能为空")
    private String userName;

    /**
     * 商品ID
     * 非空 + 最小值校验
     */
    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须大于0")
    private Long goodsId;

    /**
     * 商品名称
     * 非空校验
     */
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    /**
     * 进货价/成本价（单商品）
     * 非空 + 最小值校验
     */
    @NotNull(message = "成本价不能为空")
    @Min(value = 0, message = "成本价不能为负数")
    private BigDecimal costPrice;

    /**
     * 售价(标价)（单商品）
     * 非空 + 最小值校验
     */
    @NotNull(message = "售价不能为空")
    @Min(value = 0, message = "售价不能为负数")
    private BigDecimal salePrice;

    /**
     * 购买数量
     * 非空 + 最小值校验
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer buyQty;

    /**
     * 订单备注
     */
    private String remark;
}