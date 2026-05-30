package com.inventory.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单号生成器
 *
 * 示例：
 * RK20260530145623001
 * CK20260530145623002
 * DD20260530145623003
 */
public class OrderNoGenerator {

    /**
     * 毫秒内递增序号
     */
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /**
     * 上次生成时间
     */
    private static volatile String LAST_TIME = "";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoGenerator() {
    }

    /**
     * 生成业务单号
     *
     * @param prefix 前缀
     *               RK 入库
     *               CK 出库
     *               DD 订单
     *               CG 采购
     *               XS 销售
     *               TJ 调整
     * @return 单号
     */
    public static synchronized String generate(String prefix) {

        String currentTime =
                LocalDateTime.now().format(FORMATTER);

        if (!currentTime.equals(LAST_TIME)) {
            LAST_TIME = currentTime;
            SEQUENCE.set(0);
        }

        int seq = SEQUENCE.incrementAndGet();

        return prefix
                + currentTime
                + String.format("%03d", seq);
    }
}