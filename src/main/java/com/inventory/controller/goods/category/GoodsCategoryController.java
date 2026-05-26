package com.inventory.controller.goods.category;

import com.inventory.common.result.Result;
import com.inventory.entity.goods.GoodsCategoryDTO;
import com.inventory.service.GoodsCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品分类 API
 * 完全匹配前端 React 页面
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/goods/category")
public class GoodsCategoryController {

    private final GoodsCategoryService goodsCategoryService;

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return goodsCategoryService.page(keyword, pageNum, pageSize);
    }

    /**
     * 分类树（下拉/级联用）
     */
    @GetMapping("/tree")
    public Result<?> tree(@RequestParam(required = false) String keyword) {
        return goodsCategoryService.tree(keyword);
    }

    /**
     * 新增
     */
    @PostMapping
    public Result<?> add(@Valid @RequestBody GoodsCategoryDTO dto) {
        return goodsCategoryService.add(dto);
    }

    /**
     * 修改
     */
    @PutMapping("/{id}")
    public Result<?> update(
            @PathVariable String id,
            @Valid @RequestBody GoodsCategoryDTO dto
    ) {
        return goodsCategoryService.update(id, dto);
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        return goodsCategoryService.delete(id);
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<String> ids) {
        return goodsCategoryService.batchDelete(ids);
    }

    /**
     * 单个状态修改
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(
            @PathVariable String id,
            @RequestParam Integer status
    ) {
        return goodsCategoryService.updateStatus(id, status);
    }

    /**
     * 批量状态修改
     */
    @PutMapping("/batch/status")
    public Result<?> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        List<String> ids = (List<String>) params.get("ids");
        Integer status = (Integer) params.get("status");
        return goodsCategoryService.batchUpdateStatus(ids, status);
    }
}