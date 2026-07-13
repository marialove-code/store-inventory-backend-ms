package com.inventory.common.constants;

/**
 * 业务单号前缀常量（从单体迁入）。
 * <p>
 * 配合 {@link com.inventory.common.utils.OrderNoGenerator} 生成如 DD20260530145623001 的单号。
 * </p>
 */
public interface OrderPrefix {

    /** 入库 */
    String INBOUND = "RK";

    /** 出库 */
    String OUTBOUND = "CK";

    /** 订单 */
    String ORDER = "DD";

    /** 采购 */
    String PURCHASE = "CG";

    /** 销售 */
    String SALES = "XS";

    /** 调整 */
    String ADJUST = "TJ";
}
