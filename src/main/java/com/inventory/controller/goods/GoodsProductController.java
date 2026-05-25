package com.inventory.controller.goods;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.annotation.RequiresPerm;
import com.inventory.common.result.Result;
import com.inventory.entity.goods.GoodsProductListVO;
import com.inventory.service.GoodsProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

/**
 * 商品管理 Controller
 * 对应数据库表：goods_product
 * 只负责：接口接收 + 调用Service + 统一返回结果
 */
@RestController
@RequestMapping("/goods/product")
@RequiredArgsConstructor
public class GoodsProductController {

    private final GoodsProductService goodsProductService;

    /**
     * 商品分页列表查询
     * @param keyword      关键词：商品名称/编码
     * @param categoryId   分类ID
     * @param brandId      品牌ID
     * @param shelfStatus  上下架状态
     * @param productCode  商品编码（精确查询）
     * @param minPrice     最低售价
     * @param maxPrice     最高售价
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @return 分页商品列表
     */
    @GetMapping("/list")
    @RequiresPerm("goods:product:list")
    public Result<Page<GoodsProductListVO>> list(
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
        return Result.success(goodsProductService.pageProduct(
                keyword,
                categoryId,
                brandId,
                shelfStatus,
                productCode,
                minPrice,
                maxPrice,
                pageNum,
                pageSize
        ));
    }
}