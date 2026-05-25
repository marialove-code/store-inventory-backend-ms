package com.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.goods.GoodsProduct;
import com.inventory.entity.goods.GoodsProductListVO;
import com.inventory.service.GoodsProductService;
import com.inventory.mapper.GoodsProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 95349
 * @description 针对表【goods_product(商品主表)】的数据库操作Service实现
 * @createDate 2026-05-25 18:26:40
 * 商品 Service 实现
 * 对应表：goods_product
*/
@Service
@RequiredArgsConstructor
public class GoodsProductServiceImpl extends ServiceImpl<GoodsProductMapper, GoodsProduct>
    implements GoodsProductService{

    private final GoodsProductMapper goodsProductMapper;

    @Override
    public Page<GoodsProductListVO> pageProduct(
            String keyword,
            String categoryId,
            String brandId,
            Integer shelfStatus,
            String productCode,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long pageNum,
            Long pageSize
    ) {
        Page<GoodsProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GoodsProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GoodsProduct::getIsDeleted, 0);

        // 关键词模糊查询：商品名称 / 商品编码
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(GoodsProduct::getProductName, keyword)
                    .or().like(GoodsProduct::getProductCode, keyword)
            );
        }

        // 分类ID筛选
        if (StrUtil.isNotBlank(categoryId)) {
            wrapper.eq(GoodsProduct::getCategoryId, categoryId);
        }

        // 品牌ID筛选
        if (StrUtil.isNotBlank(brandId)) {
            wrapper.eq(GoodsProduct::getBrandId, brandId);
        }

        // 上下架状态
        if (shelfStatus != null) {
            wrapper.eq(GoodsProduct::getShelfStatus, shelfStatus);
        }

        // 精确商品编码
        if (StrUtil.isNotBlank(productCode)) {
            wrapper.eq(GoodsProduct::getProductCode, productCode);
        }

        // 价格区间：最低价
        if (minPrice != null) {
            wrapper.ge(GoodsProduct::getSalePrice, minPrice);
        }

        // 价格区间：最高价
        if (maxPrice != null) {
            wrapper.le(GoodsProduct::getSalePrice, maxPrice);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(GoodsProduct::getCreateTime);

        // 分页查询
        Page<GoodsProduct> productPage = this.page(page, wrapper);

        // ====================== 分页VO转换 ======================
        Page<GoodsProductListVO> voPage = new Page<>(
                productPage.getCurrent(),
                productPage.getSize(),
                productPage.getTotal()
        );

        List<GoodsProductListVO> voList = productPage.getRecords().stream()
                .map(p -> {
                    GoodsProductListVO vo = BeanUtil.copyProperties(p, GoodsProductListVO.class);
                    return vo;
                })
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        // ========================================================

        return voPage;
    }

}




