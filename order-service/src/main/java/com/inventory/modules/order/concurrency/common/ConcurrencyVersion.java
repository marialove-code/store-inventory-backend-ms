package com.inventory.modules.order.concurrency.common;

import lombok.Getter;

/**
 * 并发演进版本枚举（V1～V7）。
 * <p>
 * 与 {@code docs/并发演进.md} 文档及压测对比表一一对应。
 * </p>
 */
@Getter
public enum ConcurrencyVersion {

    V1("v1", "单线程基础版（无并发控制，即当前单体默认实现）"),
    V2("v2", "JUC 基础并发（synchronized / ReentrantLock / 原子类）"),
    V3("v3", "线程池（任务拆分 + 异步处理）"),
    V4("v4", "SQL 并发控制（悲观锁 / 乐观锁）"),
    V5("v5", "Redis 并发控制（分布式锁 / Lua 原子扣减）"),
    V6("v6", "MQ 最终一致性（下单与锁库存异步解耦）"),
    V7("v7", "幂等 + 补偿机制（生产级完善）");

    /** URL / Facade 使用的版本字符串 */
    private final String code;

    /** 中文说明，便于日志与文档 */
    private final String description;

    ConcurrencyVersion(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据版本字符串解析枚举，忽略大小写。
     *
     * @param code 如 v1、V5
     * @return 对应枚举；不存在则 null
     */
    public static ConcurrencyVersion fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toLowerCase();
        for (ConcurrencyVersion v : values()) {
            if (v.code.equals(normalized)) {
                return v;
            }
        }
        return null;
    }
}
