package com.inventory.modules.order.orderdelivery.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单发货管理 VO
 * 完全匹配前端列表展示字段
 */
@Data
public class OrderDeliveryVO {

    /**
     * 订单主键ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 下单用户名称
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
     * 成本价(发货页面未使用)
     */
    private BigDecimal costPrice;

    /**
     * 售价(发货页面未使用)
     */
    private BigDecimal salePrice;

    /**
     * 购买数量
     */
    private Integer buyQty;

    /**
     * 订单总金额
     */
    private BigDecimal orderAmount;

    /**
     * 订单状态 0待支付 1已支付 2已发货 3已完成 4已取消
     */
    private Integer orderStatus;

    /**
     * 订单状态文案（前端展示）
     */
    private String statusName;

    /**
     * 物流单号
     */
    private String logisticsNo;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}