package com.inventory.modules.order.orderinfo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表查询返回VO
 * 用于封装前端展示的订单列表数据
 * @author 95349
 * @date 2026-05-31
 */
@Data
public class OrderListVO {
    /**
     * 订单主键ID
     */
    private Long id;

    /**
     * 订单编号
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
     * 进货价/成本价（单商品）
     */
    private BigDecimal costPrice;

    /**
     * 售价(标价)（单商品）
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
     * 订单状态编码：0-待支付 1-已支付 2-已发货 3-已取消
     */
    private Integer orderStatus;

    /**
     * 订单状态名称（如：待支付、已支付）
     */
    private String statusName;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 物流单号
     */
    private String logisticsNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}