package com.inventory.controller.goods.product;

import com.inventory.common.result.Result;
import com.inventory.entity.goods.BatchShelfDTO;
import com.inventory.entity.goods.GoodsProductDTO;
import com.inventory.service.GoodsProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品管理控制层
 * 功能：接收前端请求 → 转发给Service → 直接返回结果
 * 特点：无任何业务逻辑，只做路由转发
 */
@RestController
@RequestMapping("/goods/product")
@RequiredArgsConstructor
public class GoodsProductController {

    /**
     * 注入商品服务
     */
    private final GoodsProductService goodsProductService;

    /**
     * 商品列表分页查询
     * 支持：关键词、分类、品牌、状态、价格区间、编码筛选
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Integer shelfStatus,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        // 直接转发，不处理任何逻辑
        return goodsProductService.pageProduct(keyword, categoryId, brandId, shelfStatus,
                productCode, minPrice, maxPrice, pageNum, pageSize);
    }

    /**
     * 新增商品
     */
    @PostMapping
    public Result<?> add(@RequestBody GoodsProductDTO dto) {
        return goodsProductService.addProduct(dto);
    }

    /**
     * 编辑商品
     * @param id 商品ID
     * @param dto 商品修改参数
     */
    @PutMapping("/{id}")
    public Result<?> edit(@PathVariable String id, @RequestBody GoodsProductDTO dto) {
        return goodsProductService.updateProduct(id, dto);
    }

    /**
     * 删除单个商品（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<?> remove(@PathVariable String id) {
        return goodsProductService.deleteProduct(id);
    }

    /**
     * 批量删除商品
     */
    @DeleteMapping("/batch")
    public Result<?> batchRemove(@RequestBody List<String> ids) {
        return goodsProductService.batchDeleteProduct(ids);
    }

    /**
     * 单个商品上下架
     * @param shelfStatus 1上架 0下架
     */
    @PutMapping("/{id}/shelf")
    public Result<?> shelf(
            @PathVariable String id,
            @RequestParam Integer shelfStatus
    ) {
        return goodsProductService.updateShelfStatus(id, shelfStatus);
    }

    /**
     * 批量上下架
     */
    @PutMapping("/batch/shelf")
    public Result<?> batchShelf(@RequestBody BatchShelfDTO dto) {
        return goodsProductService.batchUpdateShelfStatus(dto.getIds(), dto.getShelfStatus());
    }

    /**
     * 商品图片上传
     */
    @PostMapping("/uploadImage")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        return goodsProductService.uploadImage(file);
    }



}