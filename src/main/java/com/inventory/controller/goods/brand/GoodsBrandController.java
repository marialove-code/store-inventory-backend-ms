package com.inventory.controller.goods.brand;

import com.inventory.common.result.Result;
import com.inventory.entity.goods.GoodsBrandDTO;
import com.inventory.service.GoodsBrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品品牌 API
 * 1:1 匹配前端品牌页面
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/goods/brand")
public class GoodsBrandController {

    private final GoodsBrandService goodsBrandService;

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        return goodsBrandService.page(keyword, pageNum, pageSize);
    }

    /**
     * 全部品牌（下拉框用）
     */
    @GetMapping("/listAll")
    public Result<?> listAll() {
        return goodsBrandService.listAll();
    }

    /**
     * 新增品牌
     */
    @PostMapping
    public Result<?> add(@Valid @RequestBody GoodsBrandDTO dto) {
        return goodsBrandService.add(dto);
    }

    /**
     * 修改品牌
     */
    @PutMapping("/{id}")
    public Result<?> update(
            @PathVariable String id,
            @Valid @RequestBody GoodsBrandDTO dto
    ) {
        return goodsBrandService.update(id, dto);
    }

    /**
     * 删除品牌
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        return goodsBrandService.delete(id);
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<String> ids) {
        return goodsBrandService.batchDelete(ids);
    }

    /**
     * 状态修改
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(
            @PathVariable String id,
            @RequestParam Integer status
    ) {
        return goodsBrandService.updateStatus(id, status);
    }

    /**
     * 上传logo
     */
    @PostMapping("/uploadLogo")
    public Result<?> uploadLogo(@RequestParam("file") MultipartFile file) {
        return goodsBrandService.uploadLogo(file);
    }
}