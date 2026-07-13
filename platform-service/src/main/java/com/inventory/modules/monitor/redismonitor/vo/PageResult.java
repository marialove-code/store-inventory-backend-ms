package com.inventory.modules.monitor.redismonitor.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> {

    /** 总条数 */
    private long total;

    /** 当前页数据 */
    private List<T> records;

    public static <T> PageResult<T> of(long total, List<T> records) {
        PageResult<T> page = new PageResult<>();
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }
}