package com.inventory.modules.goods.product.service;

import com.inventory.common.response.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.modules.goods.product.entity.GoodsProduct;
import com.inventory.modules.goods.product.dto.GoodsProductDTO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
* @author 95349
* @description 针对表【goods_product(商品主表)】的数据库操作Service
* @createDate 2026-05-25 18:26:40
*/
public interface GoodsProductService extends IService<GoodsProduct> {

    /**
     * 分页查询商品列表
     */
    Result<?> pageProduct(String keyword, String categoryId, String brandId, Integer shelfStatus,
                          String productCode, BigDecimal minPrice, BigDecimal maxPrice,
                          Long pageNum, Long pageSize);

    /**
     * 新增商品
     */
    Result<?> addProduct(GoodsProductDTO dto);

    /**
     * 修改商品
     */
    Result<?> updateProduct(String id, GoodsProductDTO dto);

    /**
     * 删除商品
     */
    Result<?> deleteProduct(String id);

    /**
     * 批量删除
     */
    Result<?> batchDeleteProduct(List<String> ids);

    /**
     * 上下架
     */
    Result<?> updateShelfStatus(String id, Integer shelfStatus);

    /**
     * 批量上下架
     */
    Result<?> batchUpdateShelfStatus(List<String> ids, Integer shelfStatus);

    /**
     * 图片上传
     */
    Result<?> uploadImage(MultipartFile file);

}
