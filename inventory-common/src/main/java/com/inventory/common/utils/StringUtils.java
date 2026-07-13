package com.inventory.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 字符串工具（基于 Hutool 封装常用判空与裁剪，避免业务层散落重复逻辑）。
 *
 * @author inventory
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 是否为空（null 或仅空白）
     */
    public static boolean isBlank(CharSequence cs) {
        return StrUtil.isBlank(cs);
    }

    /**
     * 是否非空
     */
    public static boolean isNotBlank(CharSequence cs) {
        return StrUtil.isNotBlank(cs);
    }

    /**
     * null 安全裁剪两端空白
     */
    public static String trim(String str) {
        return StrUtil.trim(str);
    }

    /**
     * 默认值：空则返回默认串
     */
    public static String blankToDefault(CharSequence str, String defaultStr) {
        return StrUtil.blankToDefault(str, defaultStr);
    }
}
