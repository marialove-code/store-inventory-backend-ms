package com.inventory.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

/**
 * 通用分页结果封装
 * 给前端统一返回：列表 + 总数 + 当前页 + 每页条数
 * @param <T> 列表数据泛型
 */
@Data
public class PageResult<T> {

    /**
     * 数据列表（记录）
     */
    private List<T> records;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页条数
     */
    private Long size;

    /**
     * 从 MyBatis-Plus 的 IPage 构建分页对象
     */
    public static <T> PageResult<T> build(IPage<?> page, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }
}