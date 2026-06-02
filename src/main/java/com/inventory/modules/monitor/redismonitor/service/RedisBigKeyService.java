package com.inventory.modules.monitor.redismonitor.service;
import com.inventory.modules.monitor.redismonitor.vo.PageResult;
import com.inventory.modules.monitor.redismonitor.entity.BigKeyItem;

/**
 * Redis 大 Key 服务
 */
public interface RedisBigKeyService {

    /**
     * 分页获取大 Key
     *
     * @param page 当前页码，从1开始
     * @param size 每页条数
     * @return 分页列表
     */
    PageResult<BigKeyItem> getBigKeyPage(int page, int size);
}