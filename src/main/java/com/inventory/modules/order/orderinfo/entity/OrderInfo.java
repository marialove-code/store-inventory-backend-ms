package com.inventory.modules.order.orderinfo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息表实体类
 * 对应数据库表：order_info
 * @author 95349
 * @date 2026-05-31
 */
@TableName(value = "order_info")
@Data
public class OrderInfo {
    /**
     * 订单主键ID
     */
    @TableId
    private Long id;

    /**
     * 订单编号（唯一，由生成器生成）
     */
    private String orderNo;

    /**
     * 用户ID（关联用户表）
     */
    private Long userId;

    /**
     * 下单用户名称
     */
    private String userName;

    /**
     * 商品ID（关联商品表）
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
     * 订单总金额（售价 * 购买数量）
     */
    private BigDecimal orderAmount;

    /**
     * 订单状态：0-待支付 1-已支付 2-已发货 3-已取消
     * 关联枚举：OrderStatusEnum
     */
    private Integer orderStatus;

    /**
     * 支付时间（订单支付成功后填充）
     */
    private LocalDateTime payTime;

    /**
     * 物流单号（发货后填充）
     */
    private String logisticsNo;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 创建时间（订单生成时间）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（订单信息更新时间）
     */
    private LocalDateTime updateTime;
}