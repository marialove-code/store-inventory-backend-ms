package com.inventory.modules.order.orderdelivery.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发货单实体（对应 order_delivery）。
 */
@TableName(value = "order_delivery")
@Data
public class OrderDelivery {

    @TableId
    private Long id;

    private String orderNo;
    private Long userId;
    private String userName;
    private Long goodsId;
    private String goodsName;
    private Integer buyQty;
    private BigDecimal orderAmount;

    /** 发货单状态：1-待发货 2-已发货 3-已收货 4-退款中 等 */
    private Integer orderStatus;

    private String logisticsNo;
    private String remark;
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
