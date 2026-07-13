package com.inventory.modules.order.orderinfo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息表实体（对应 order_info）。
 */
@TableName(value = "order_info")
@Data
public class OrderInfo {

    /** 订单主键 ID */
    @TableId
    private Long id;

    /** 订单编号（唯一） */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 下单用户名称 */
    private String userName;

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 进货价/成本价（单商品） */
    private BigDecimal costPrice;

    /** 售价（单商品） */
    private BigDecimal salePrice;

    /** 购买数量 */
    private Integer buyQty;

    /** 订单总金额（售价 * 数量） */
    private BigDecimal orderAmount;

    /** 订单状态，见 OrderStatusEnum */
    private Integer orderStatus;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 物流单号 */
    private String logisticsNo;

    /** 订单备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;
}
