package com.inventory.modules.order.concurrency.common;

/**
 * 并发压测专用常量。
 * <p>
 * 压测前请确认：商品库存、锁定库存已重置；测试订单已清理（可选）。
 * </p>
 */
public final class ConcurrencyTestConstants {

    private ConcurrencyTestConstants() {
    }

    /**
     * 默认压测商品：小米17（可在 reset 接口传入其他 goodsId 覆盖）。
     */
    public static final Long DEFAULT_TEST_GOODS_ID = 2064625692771397632L;

    /**
     * 压测默认初始「当前库存」。
     */
    public static final int DEFAULT_INITIAL_STOCK = 100;

    /**
     * 压测默认初始「锁定库存」（重置时应归零）。
     */
    public static final int DEFAULT_INITIAL_LOCK_STOCK = 0;

    /**
     * 文档计划中的默认并发线程数（JMeter / JUnit 参考值）。
     */
    public static final int DEFAULT_CONCURRENT_THREADS = 200;

    /**
     * 每笔订单默认购买数量。
     */
    public static final int DEFAULT_BUY_QTY = 1;
}
