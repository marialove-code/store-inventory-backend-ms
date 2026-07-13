package com.inventory.modules.order.orderinfo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表查询返回 VO。
 */
@Data
public class OrderListVO {

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
    /** 状态中文名 */
    private String statusName;
    private LocalDateTime payTime;
    private String logisticsNo;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
