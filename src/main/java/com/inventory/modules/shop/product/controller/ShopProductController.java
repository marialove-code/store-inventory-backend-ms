package com.inventory.modules.shop.product.controller;


import com.inventory.common.page.PageResult;
import com.inventory.common.response.Result;
import com.inventory.modules.shop.product.dto.ShopProductCreateDto;
import com.inventory.modules.shop.product.dto.ShopProductUpdateDto;
import com.inventory.modules.shop.product.entity.ShopProductListParam;
import com.inventory.modules.shop.product.service.ShopProductService;
import com.inventory.modules.shop.product.vo.ShopProductOptionVo;
import com.inventory.modules.shop.product.vo.ShopProductVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品配件管理
 */
@RestController
@RequestMapping("/shop/product")
@RequiredArgsConstructor
public class ShopProductController {

    private final ShopProductService shopProductService;

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result<?> list(ShopProductListParam param) {
        return shopProductService.getProductPage(param);
    }

    /**
     * 下拉选项（开单用）
     */
    @GetMapping("/options")
    public Result<?> options() {
        return shopProductService.getProductOptions();
    }

    /**
     * 库存金额汇总（总进价、总售价）
     */
    @GetMapping("/stats")
    public Result<?> stats() {
        return shopProductService.getProductStats();
    }

    /**
     * 新增商品
     */
    @PostMapping
    public Result<?> create(@Valid @RequestBody ShopProductCreateDto dto) {
        return shopProductService.createProduct(dto);
    }

    /**
     * 修改（全字段）
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id,
                                  @Valid @RequestBody ShopProductUpdateDto dto) {
        return shopProductService.updateProduct(id, dto);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return shopProductService.deleteProduct(id);
    }
}