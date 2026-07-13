package com.inventory.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import cn.hutool.core.date.DateUtil;

/**
 * 日期时间工具（基于 Hutool 与 JDK 时间 API）。
 *
 * @author inventory
 */
public final class DateUtils {

    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);

    private DateUtils() {
    }

    /**
     * 当前时间
     */
    public static Date now() {
        return new Date();
    }

    /**
     * 格式化 JDK8 时间
     */
    public static String format(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return DEFAULT_FORMATTER.format(time);
    }

    /**
     * 格式化 java.util.Date
     */
    public static String format(Date date, String pattern) {
        return DateUtil.format(date, pattern);
    }

    /**
     * 解析为 java.util.Date
     */
    public static Date parse(String dateStr, String pattern) {
        return DateUtil.parse(dateStr, pattern);
    }
}
