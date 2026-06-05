package com.inventory.modules.shop.product.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.page.PageResult;
import com.inventory.common.response.Result;
import com.inventory.modules.shop.dashboard.vo.ShopStockWarnVO;
import com.inventory.modules.shop.product.dto.ShopProductCreateDto;
import com.inventory.modules.shop.product.dto.ShopProductUpdateDto;
import com.inventory.modules.shop.product.entity.ShopProduct;
import com.inventory.modules.shop.product.entity.ShopProductListParam;
import com.inventory.modules.shop.product.mapper.ShopProductMapper;
import com.inventory.modules.shop.product.service.ShopProductService;
import com.inventory.modules.shop.product.vo.ShopProductOptionVo;
import com.inventory.modules.shop.product.vo.ShopProductVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopProductServiceImpl extends ServiceImpl<ShopProductMapper, ShopProduct>
        implements ShopProductService {

    private final ShopProductMapper shopProductMapper;

    /**
     * 分页查询商品
     */
    @Override
    public Result<?> getProductPage(ShopProductListParam param) {
        Page<ShopProduct> page = new Page<>(param.getPageNum(), param.getPageSize());

        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();

        // 商品名称模糊搜索
        if (StrUtil.isNotBlank(param.getKeyword())) {
            wrapper.like(ShopProduct::getProductName, param.getKeyword());
        }

        wrapper.orderByDesc(ShopProduct::getCreateTime);

        Page<ShopProduct> productPage = this.page(page, wrapper);

        Page<ShopProductVo> voPage = new Page<>(
                productPage.getCurrent(),
                productPage.getSize(),
                productPage.getTotal()
        );

        List<ShopProductVo> voList = productPage.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, ShopProductVo.class))
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 开单下拉选项
     */
    @Override
    public Result<?> getProductOptions() {
        List<ShopProduct> list = this.list();
        List<ShopProductOptionVo> voList = list.stream().map(item -> {
            ShopProductOptionVo vo = new ShopProductOptionVo();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 新增商品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createProduct(ShopProductCreateDto dto) {
        ShopProduct product = new ShopProduct();
        BeanUtils.copyProperties(dto, product);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        boolean save = this.save(product);
        return Result.success(save);
    }

    /**
     * 修改商品（补货+调价）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateProduct(Long id, ShopProductUpdateDto dto) {
        ShopProduct product = this.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        // 补货
        if (dto.getReceiptQty() > 0) {
            product.setStock(product.getStock() + dto.getReceiptQty());
        }

        // 调价
        product.setCostPrice(dto.getCostPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setUpdateTime(LocalDateTime.now());

        boolean update = this.updateById(product);
        return Result.success(update);
    }


    @Override
    public List<ShopStockWarnVO> getWarnStockList() {
        return baseMapper.selectWarnProduct();
    }
}