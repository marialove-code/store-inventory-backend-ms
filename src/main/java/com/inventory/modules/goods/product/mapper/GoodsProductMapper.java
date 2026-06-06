package com.inventory.modules.goods.product.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.modules.goods.product.entity.GoodsProduct;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.goods.product.vo.GoodsProductListVO;
import org.apache.ibatis.annotations.Param;

/**
* @author 95349
* @description 针对表【goods_product(商品信息表)】的数据库操作Mapper
* @createDate 2026-05-26 18:21:19
* @Entity com.inventory.modules.goods.product.entity.GoodsProduct
*/
public interface GoodsProductMapper extends BaseMapper<GoodsProduct> {

    /**
     * 商品+库存联查分页，所有参数全部传入XML动态拼接
     */
    Page<GoodsProductListVO> selectProductWithStock(Page<GoodsProductListVO> page,
                                                    @Param("keyword") String keyword,
                                                    @Param("categoryId") String categoryId,
                                                    @Param("brandId") String brandId,
                                                    @Param("shelfStatus") Integer shelfStatus,
                                                    @Param("productCode") String productCode,
                                                    @Param("minPrice") java.math.BigDecimal minPrice,
                                                    @Param("maxPrice") java.math.BigDecimal maxPrice);



}




