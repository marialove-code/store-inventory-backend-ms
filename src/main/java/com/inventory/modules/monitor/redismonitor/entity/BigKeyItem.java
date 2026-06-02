package com.inventory.modules.monitor.redismonitor.entity;

import lombok.Data;

/**
 * Redis 大 Key 信息
 */
@Data
public class BigKeyItem {

    /** Key 名称 */
    private String key;

    /** 数据类型 string / hash / list / set / zset */
    private String type;

    /** Key 大小，单位字节 */
    private long size;

    /** Key 剩余 TTL，单位秒 */
    private long ttl;
}