package com.inventory.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单号生成器（从单体迁入，纯 JDK，无 hutool 依赖）。
 * <p>
 * 示例：
 * <ul>
 *   <li>RK20260530145623001</li>
 *   <li>CK20260530145623002</li>
 *   <li>DD20260530145623003</li>
 * </ul>
 * 同一秒内序号递增；跨秒重置。本阶段为进程内生成，多实例部署时需改为雪花/Redis 等。
 * </p>
 */
public class OrderNoGenerator {

    /** 毫秒/秒内递增序号 */
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /** 上次生成时间（yyyyMMddHHmmss） */
    private static volatile String LAST_TIME = "";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoGenerator() {
    }

    /**
     * 生成业务单号。
     *
     * @param prefix 前缀，见 {@link com.inventory.common.constants.OrderPrefix}
     * @return 前缀 + 时间 + 三位序号
     */
    public static synchronized String generate(String prefix) {
        String currentTime = LocalDateTime.now().format(FORMATTER);

        if (!currentTime.equals(LAST_TIME)) {
            LAST_TIME = currentTime;
            SEQUENCE.set(0);
        }

        int seq = SEQUENCE.incrementAndGet();

        return prefix + currentTime + String.format("%03d", seq);
    }
}
