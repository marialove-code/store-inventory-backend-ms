package com.inventory.order.config;

/**
 * Sentinel 资源名常量（须与 {@code @SentinelResource#value}、规则配置一致）。
 */
public final class SentinelResourceNames {

    private SentinelResourceNames() {
    }

    /** 探活限流演示（保留作参考） */
    public static final String ORDER_PING = "orderPing";

    /** 熔断演示接口（保留作参考） */
    public static final String ORDER_UNSTABLE = "orderUnstable";

    /** 真实业务：下单入口 */
    public static final String ORDER_CREATE = "orderCreate";

    /** 真实业务：取消订单入口 */
    public static final String ORDER_CANCEL = "orderCancel";

    /** 真实业务：远程锁库存（Feign） */
    public static final String INVENTORY_LOCK = "inventoryLock";

    /** 真实业务：远程解锁库存（Feign） */
    public static final String INVENTORY_UNLOCK = "inventoryUnlock";

    /** 真实业务：远程查询可用库存（下单前会先调） */
    public static final String INVENTORY_USABLE = "inventoryUsable";
}
