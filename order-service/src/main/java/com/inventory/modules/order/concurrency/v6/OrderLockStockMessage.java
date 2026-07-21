package com.inventory.modules.order.concurrency.v6;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V6 发到 RabbitMQ 的消息体（下单异步锁库存）。
 * <p>
 * 字段与 {@link com.inventory.modules.order.orderinfo.dto.OrderInfoDTO} 对齐，
 * 便于生产者从 HTTP 入参拷贝、消费者再转回 DTO 调建单逻辑。
 * </p>
 */
@Data
public class OrderLockStockMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下单用户名 */
    private String userName;

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 购买数量 */
    private Integer buyQty;

    /** 备注（压测可写 JMeter V6） */
    private String remark;

    /**
     * 消息投递时间戳（毫秒），方便日志排查，不做业务强依赖。
     */
    private Long sentAt;

    /**
     * 幂等键（可选）。有值时消费者走 V7 幂等，防止重复投递重复锁库存。
     */
    private String idempotentKey;
}
