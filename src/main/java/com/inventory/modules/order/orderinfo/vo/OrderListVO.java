package com.inventory.modules.order.orderinfo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表VO
 */
@Data
public class OrderListVO {

    /** 订单主键 */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 下单用户 */
    private String userName;

    /** 商品ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 进货价/成本价 */
    private BigDecimal costPrice;

    /** 售价(标价) */
    private BigDecimal salePrice;

    /** 购买数量 */
    private Integer buyQty;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 订单状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 */
    private Integer orderStatus;

    /** 状态描述 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 */
    private String statusName;

    /** 物流单号 */
    private String logisticsNo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}