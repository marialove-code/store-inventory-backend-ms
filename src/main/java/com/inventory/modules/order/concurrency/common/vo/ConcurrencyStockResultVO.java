package com.inventory.modules.order.concurrency.common.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 压测结果查询 VO。
 * <p>
 * 用于 {@code GET /order/concurrency/stock/result}，压测结束后核对是否超锁。
 * </p>
 */
@Data
@Builder
public class ConcurrencyStockResultVO {

    /** 商品 ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 当前库存（实物库存，下单锁定阶段通常不变） */
    private Integer stock;

    /** 锁定库存（并发实验核心观察指标） */
    private Integer lockStock;

    /** 可用库存 = stock - lockStock */
    private Integer usableStock;

    /**
     * 该商品「待支付」状态订单数（可选统计，实现后用于与 lockStock 交叉验证）。
     * <p>
     * TODO: 在 {@link com.inventory.modules.order.concurrency.common.ConcurrencyTestHelper} 中补充统计逻辑。
     * </p>
     */
    private Long pendingOrderCount;

    /** 是否疑似超锁：lockStock &gt; stock 或 usableStock &lt; 0 */
    private Boolean overLocked;
}
