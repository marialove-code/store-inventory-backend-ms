package com.inventory.modules.shop.product.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.inventory.modules.shop.product.vo.ShopProductStatsVo;
import com.inventory.modules.shop.product.vo.ShopProductVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        wrapper.eq(ShopProduct::getIsDeleted, 0);

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
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopProduct::getIsDeleted, 0);
        wrapper.orderByDesc(ShopProduct::getCreateTime);
        List<ShopProduct> list = this.list(wrapper);
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
        product.setIsDeleted(0);
        boolean save = this.save(product);
        return Result.success(save);
    }

    /**
     * 修改商品（全字段可改）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateProduct(Long id, ShopProductUpdateDto dto) {
        ShopProduct product = this.getById(id);
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }

        product.setProductName(dto.getProductName());
        product.setStock(dto.getStock());
        product.setCostPrice(dto.getCostPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setFactory(dto.getFactory());
        product.setFactoryContact(dto.getFactoryContact());
        product.setRemark(dto.getRemark());
        product.setUpdateTime(LocalDateTime.now());

        boolean update = this.updateById(product);
        return Result.success(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteProduct(Long id) {
        ShopProduct product = this.getById(id);
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }
        if (product.getStock() != null && product.getStock() > 0) {
            return Result.fail("删除失败：该商品尚有库存，无法删除");
        }

        LambdaUpdateWrapper<ShopProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShopProduct::getId, id);
        wrapper.set(ShopProduct::getIsDeleted, 1);
        wrapper.set(ShopProduct::getUpdateTime, LocalDateTime.now());
        this.update(wrapper);
        return Result.success("删除成功");
    }

    @Override
    public Result<?> getProductStats() {
        ShopProductStatsVo stats = shopProductMapper.selectProductInventoryStats();
        if (stats == null) {
            stats = new ShopProductStatsVo();
        }
        if (stats.getTotalCostAmount() == null) {
            stats.setTotalCostAmount(BigDecimal.ZERO);
        }
        if (stats.getTotalSaleAmount() == null) {
            stats.setTotalSaleAmount(BigDecimal.ZERO);
        }
        return Result.success(stats);
    }


    @Override
    public List<ShopStockWarnVO> getWarnStockList() {
        return baseMapper.selectWarnProduct();
    }
}