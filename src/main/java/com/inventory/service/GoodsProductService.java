package com.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.goods.GoodsProduct;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.entity.goods.GoodsProductListVO;

import java.math.BigDecimal;

/**
* @author 95349
* @description 针对表【goods_product(商品主表)】的数据库操作Service
* @createDate 2026-05-25 18:26:40
*/
public interface GoodsProductService extends IService<GoodsProduct> {
    /**
     * 商品分页条件查询
     */
    Page<GoodsProductListVO> pageProduct(
            String keyword,
            String categoryId,
            String brandId,
            Integer shelfStatus,
            String productCode,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long pageNum,
            Long pageSize
    );
}
