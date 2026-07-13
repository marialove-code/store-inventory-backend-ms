package com.inventory.modules.order.orderdelivery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发货管理列表 VO。
 */
@Data
public class OrderDeliveryVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private String userName;
    private Long goodsId;
    private String goodsName;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private Integer buyQty;
    private BigDecimal orderAmount;
    private Integer orderStatus;
    private String statusName;
    private String logisticsNo;
    private String remark;
    private LocalDateTime createTime;
}
