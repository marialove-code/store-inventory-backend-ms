package com.inventory.common.constants;

/**
 * 全局通用常量（与具体业务无关）。
 *
 * @author inventory
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /**
     * 逻辑删除：未删除
     */
    public static final int NOT_DELETED = 0;

    /**
     * 逻辑删除：已删除
     */
    public static final int DELETED = 1;

    /**
     * 默认分页：页码从 1 开始
     */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /**
     * 默认分页大小
     */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /**
     * 最大分页大小（防止误传超大值压垮数据库）
     */
    public static final long MAX_PAGE_SIZE = 500L;
}
