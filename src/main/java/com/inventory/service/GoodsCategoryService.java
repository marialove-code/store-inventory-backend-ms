package com.inventory.service;

import com.inventory.common.result.Result;
import com.inventory.entity.goods.GoodsCategory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.entity.goods.GoodsCategoryDTO;

import java.util.List;

/**
* @author 95349
* @description 针对表【goods_category(商品分类表)】的数据库操作Service
* @createDate 2026-05-26 19:13:00
*/
public interface GoodsCategoryService extends IService<GoodsCategory> {


    /**
     * 分页查询分类列表
     */
    Result<?> page(String keyword, Long pageNum, Long pageSize);

    /**
     * 查询分类树（用于下拉、级联选择）
     */
    Result<?> tree(String keyword);

    /**
     * 新增分类
     */
    Result<?> add(GoodsCategoryDTO dto);

    /**
     * 修改分类
     */
    Result<?> update(String id, GoodsCategoryDTO dto);

    /**
     * 删除分类
     */
    Result<?> delete(String id);

    /**
     * 批量删除分类
     */
    Result<?> batchDelete(List<String> ids);

    /**
     * 修改状态（启用/禁用）
     */
    Result<?> updateStatus(String id, Integer status);

    /**
     * 批量修改状态
     */
    Result<?> batchUpdateStatus(List<String> ids, Integer status);
}
