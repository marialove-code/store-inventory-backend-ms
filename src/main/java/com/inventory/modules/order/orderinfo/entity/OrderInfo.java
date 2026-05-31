package com.inventory.modules.order.orderinfo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 订单信息表
 * @TableName order_info
 */
@TableName(value ="order_info")
@Data
public class OrderInfo {
    /**
     * 订单主键
     */
    @TableId
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
     * 订单状态：0-待支付 1-已支付 2-已发货 3-已取消
     */
    private Integer orderStatus;

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