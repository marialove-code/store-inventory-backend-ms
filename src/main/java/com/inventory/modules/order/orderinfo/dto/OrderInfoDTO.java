package com.inventory.modules.order.orderinfo.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 新增订单DTO
 */
@Data
public class OrderInfoDTO {

    /**
     * 下单用户
     */
    private String userName;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 进货价/成本价
     */
    private BigDecimal costPrice;

    /**
     * 售价(标价)
     */
    private BigDecimal salePrice;

    /**
     * 购买数量
     */
    private Integer buyQty;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 备注
     */
    private String remark;
}