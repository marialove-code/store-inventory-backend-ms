package com.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.result.Result;
import com.inventory.entity.goods.GoodsProduct;
import com.inventory.entity.goods.GoodsProductDTO;
import com.inventory.entity.goods.GoodsProductListVO;
import com.inventory.service.GoodsProductService;
import com.inventory.mapper.GoodsProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * 注入MP基础Mapper，自带所有增删改查
     */
    private final GoodsProductMapper goodsProductMapper;

    /**
     * 图片上传存储路径
     */
    @Value("${app.upload.product-image-path}")
    private String productImagePath;

    /**
     * 图片访问前缀
     */
    @Value("${app.upload.product-image-prefix}")
    private String imagePrefix;

    // ======================== 1. 分页查询商品 ========================
    /**
     * 分页 + 多条件查询
     * 支持：关键词、分类、品牌、上下架、编码、价格区间
     */
    @Override
    public Result<?> pageProduct(String keyword, String categoryId, String brandId, Integer shelfStatus,
                                 String productCode, BigDecimal minPrice, BigDecimal maxPrice,
                                 Long pageNum, Long pageSize) {

        // 1. 构建分页对象
        Page<GoodsProduct> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件
        LambdaQueryWrapper<GoodsProduct> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GoodsProduct::getIsDeleted, 0);

        // 关键词模糊查询（名称 + 编码）
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(GoodsProduct::getProductName, keyword)
                    .or()
                    .like(GoodsProduct::getProductCode, keyword)
            );
        }

        // 分类ID
        if (StrUtil.isNotBlank(categoryId)) {
            wrapper.eq(GoodsProduct::getCategoryId, Long.valueOf(categoryId));
        }

        // 品牌ID
        if (StrUtil.isNotBlank(brandId)) {
            wrapper.eq(GoodsProduct::getBrandId, Long.valueOf(brandId));
        }

        // 上下架状态
        if (shelfStatus != null) {
            wrapper.eq(GoodsProduct::getShelfStatus, shelfStatus);
        }

        // 商品编码精确查询
        if (StrUtil.isNotBlank(productCode)) {
            wrapper.eq(GoodsProduct::getProductCode, productCode);
        }

        // 价格 >= 最低价
        if (minPrice != null) {
            wrapper.ge(GoodsProduct::getSalePrice, minPrice);
        }

        // 价格 <= 最高价
        if (maxPrice != null) {
            wrapper.le(GoodsProduct::getSalePrice, maxPrice);
        }

        // 排序：按 sort 正序 + 时间倒序（最合理）
        wrapper.orderByAsc(GoodsProduct::getSort);
        wrapper.orderByDesc(GoodsProduct::getCreateTime);

        // 3. 执行MP分页查询
        Page<GoodsProduct> productPage = goodsProductMapper.selectPage(page, wrapper);

        // ====================== VO 转换（昨天的标准写法） ======================
        Page<GoodsProductListVO> voPage = new Page<>(
                productPage.getCurrent(),
                productPage.getSize(),
                productPage.getTotal()
        );

        List<GoodsProductListVO> voList = productPage.getRecords().stream()
                .map(p -> {
                    GoodsProductListVO vo = new GoodsProductListVO();
                    // 自动拷贝同名字段
                    BeanUtil.copyProperties(p, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        // ====================================================================

        // 4. 包装Result返回
        return Result.success(voPage);
    }

    // ======================== 2. 新增商品 ========================
    /**
     * 新增商品
     * 校验商品编码唯一 + DTO转实体 + 保存图片列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addProduct(GoodsProductDTO dto) {

        // 1. 校验：商品编码唯一
        if (StrUtil.isNotBlank(dto.getProductCode())) {
            Long count = goodsProductMapper.selectCount(new LambdaQueryWrapper<GoodsProduct>()
                    .eq(GoodsProduct::getProductCode, dto.getProductCode())
                    .eq(GoodsProduct::getIsDeleted, 0));

            if (count > 0) {
                return Result.fail("商品编码已存在，不允许重复添加");
            }
        }

        // 2. DTO 转换成 数据库实体
        GoodsProduct product = new GoodsProduct();

        // 雪花算法生成Long类型ID（对应PG int8）
        product.setId(IdUtil.getSnowflakeNextId());

        // 基本信息赋值
        product.setProductName(dto.getProductName());
        product.setSpecModel(dto.getSpecModel());
        product.setProductCode(dto.getProductCode());
        product.setCategoryId(StrUtil.isBlank(dto.getCategoryId()) ? null : Long.valueOf(dto.getCategoryId()));
        product.setCategoryName(dto.getCategoryName());
        product.setBrandId(StrUtil.isBlank(dto.getBrandId()) ? null : Long.valueOf(dto.getBrandId()));
        product.setBrandName(dto.getBrandName());
        product.setSupplierName(dto.getSupplierName());
        product.setManufacturer(dto.getManufacturer());
        product.setUnit(dto.getUnit());

        // 价格信息
        product.setCostPrice(dto.getCostPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setActualSalePrice(dto.getActualSalePrice());

        // 库存信息
        product.setStock(dto.getStock() == null ? 0 : dto.getStock());
        product.setStockWarn(dto.getStockWarn() == null ? 0 : dto.getStockWarn());

        // 位置与状态
        product.setShowcasePosition(dto.getShowcasePosition());
        product.setShelfStatus(dto.getShelfStatus());
        product.setSort(dto.getSort() == null ? 0 : dto.getSort());

        // 3. 处理图片列表（前端传List，数据库存JSON字符串）
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            product.setImages(JSONUtil.toJsonStr(dto.getImages()));
            product.setMainImage(dto.getMainImage() == null ? dto.getImages().get(0) : dto.getMainImage());
        } else {
            product.setMainImage(dto.getMainImage());
            product.setImages(null);
        }

        // 4. 系统默认字段
        product.setIsDeleted(0);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        // 5. MP 内置插入方法
        goodsProductMapper.insert(product);

        return Result.success("商品新增成功");
    }

    // ======================== 3. 修改商品 ========================
    /**
     * 根据ID修改商品
     * 校验商品存在 + 编码唯一 + 动态更新字段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateProduct(String id, GoodsProductDTO dto) {

        Long longId = Long.valueOf(id);

        // 1. 校验商品是否存在（且未删除）
        GoodsProduct oldProduct = goodsProductMapper.selectById(longId);
        if (oldProduct == null || oldProduct.getIsDeleted() == 1) {
            return Result.fail("商品不存在或已删除");
        }

        // 2. 校验商品编码是否重复（排除自身）
        if (StrUtil.isNotBlank(dto.getProductCode())) {
            Long count = goodsProductMapper.selectCount(new LambdaQueryWrapper<GoodsProduct>()
                    .eq(GoodsProduct::getProductCode, dto.getProductCode())
                    .ne(GoodsProduct::getId, longId)
                    .eq(GoodsProduct::getIsDeleted, 0));

            if (count > 0) {
                return Result.fail("商品编码已被其他商品使用");
            }
        }

        // 3. 构建更新条件（只更新有值的字段，更安全）
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsProduct::getId, longId);

        // 有值才更新，没值不动
        wrapper.set(StrUtil.isNotBlank(dto.getProductName()), GoodsProduct::getProductName, dto.getProductName());
        wrapper.set(StrUtil.isNotBlank(dto.getSpecModel()), GoodsProduct::getSpecModel, dto.getSpecModel());
        wrapper.set(StrUtil.isNotBlank(dto.getCategoryId()), GoodsProduct::getCategoryId, Long.valueOf(dto.getCategoryId()));
        wrapper.set(StrUtil.isNotBlank(dto.getCategoryName()), GoodsProduct::getCategoryName, dto.getCategoryName());
        wrapper.set(StrUtil.isNotBlank(dto.getBrandId()), GoodsProduct::getBrandId, Long.valueOf(dto.getBrandId()));
        wrapper.set(StrUtil.isNotBlank(dto.getBrandName()), GoodsProduct::getBrandName, dto.getBrandName());
        wrapper.set(StrUtil.isNotBlank(dto.getSupplierName()), GoodsProduct::getSupplierName, dto.getSupplierName());
        wrapper.set(StrUtil.isNotBlank(dto.getManufacturer()), GoodsProduct::getManufacturer, dto.getManufacturer());
        wrapper.set(StrUtil.isNotBlank(dto.getUnit()), GoodsProduct::getUnit, dto.getUnit());

        // 价格
        wrapper.set(dto.getCostPrice() != null, GoodsProduct::getCostPrice, dto.getCostPrice());
        wrapper.set(dto.getSalePrice() != null, GoodsProduct::getSalePrice, dto.getSalePrice());
        wrapper.set(dto.getActualSalePrice() != null, GoodsProduct::getActualSalePrice, dto.getActualSalePrice());

        // 库存
        wrapper.set(dto.getStock() != null, GoodsProduct::getStock, dto.getStock());
        wrapper.set(dto.getStockWarn() != null, GoodsProduct::getStockWarn, dto.getStockWarn());

        // 位置、状态、排序
        wrapper.set(StrUtil.isNotBlank(dto.getShowcasePosition()), GoodsProduct::getShowcasePosition, dto.getShowcasePosition());
        wrapper.set(dto.getShelfStatus() != null, GoodsProduct::getShelfStatus, dto.getShelfStatus());
        wrapper.set(dto.getSort() != null, GoodsProduct::getSort, dto.getSort());

        // 图片更新
        if (dto.getImages() != null) {
            wrapper.set(GoodsProduct::getImages, JSONUtil.toJsonStr(dto.getImages()));
        }
        if (StrUtil.isNotBlank(dto.getMainImage())) {
            wrapper.set(GoodsProduct::getMainImage, dto.getMainImage());
        }

        // 自动更新时间
        wrapper.set(GoodsProduct::getUpdateTime, LocalDateTime.now());

        // 4. MP 执行更新
        goodsProductMapper.update(null, wrapper);

        return Result.success("商品修改成功");
    }

    // ======================== 4. 删除商品（逻辑删除） ========================
    /**
     * 根据ID单个删除（逻辑删除 is_deleted=1）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteProduct(String id) {

        Long longId = Long.valueOf(id);

        // 1. 校验是否存在
        GoodsProduct product = goodsProductMapper.selectById(longId);
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }

        // 2. 执行逻辑删除
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsProduct::getId, longId).set(GoodsProduct::getIsDeleted, 1);

        goodsProductMapper.update(null, wrapper);

        return Result.success("商品删除成功");
    }

    // ======================== 5. 批量删除 ========================
    /**
     * 批量逻辑删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchDeleteProduct(List<String> ids) {

        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的商品");
        }

        List<Long> longIds = ids.stream().map(Long::valueOf).toList();

        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsProduct::getId, longIds).set(GoodsProduct::getIsDeleted, 1);

        goodsProductMapper.update(null, wrapper);

        return Result.success("批量删除成功");
    }

    // ======================== 6. 单个上下架 ========================
    /**
     * 修改上下架状态 0=下架 1=上架
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateShelfStatus(String id, Integer shelfStatus) {

        Long longId = Long.valueOf(id);

        // 校验状态
        if (shelfStatus != 0 && shelfStatus != 1) {
            return Result.fail("上下架状态不合法");
        }

        // 校验商品
        GoodsProduct product = goodsProductMapper.selectById(longId);
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }

        // 更新状态
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsProduct::getId, longId).set(GoodsProduct::getShelfStatus, shelfStatus);

        goodsProductMapper.update(null, wrapper);

        String msg = shelfStatus == 1 ? "商品上架成功" : "商品下架成功";
        return Result.success(msg);
    }

    // ======================== 7. 批量上下架 ========================
    /**
     * 批量修改上下架
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchUpdateShelfStatus(List<String> ids, Integer shelfStatus) {

        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择商品");
        }
        if (shelfStatus != 0 && shelfStatus != 1) {
            return Result.fail("状态不合法");
        }

        List<Long> longIds = ids.stream().map(Long::valueOf).toList();

        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsProduct::getId, longIds).set(GoodsProduct::getShelfStatus, shelfStatus);

        goodsProductMapper.update(null, wrapper);

        return Result.success("批量操作成功");
    }

    // ======================== 8. 图片上传 ========================
    /**
     * 上传商品图片
     */
    @Override
    public Result<?> uploadImage(MultipartFile file) {
        try {
            // 文件名：雪花ID + 后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = IdUtil.getSnowflakeNextId() + suffix;

            // 创建目录
            File dir = new File(productImagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 写入文件
            file.transferTo(new File(dir, fileName));

            // 返回可访问地址
            String fileUrl = imagePrefix + fileName;
            return Result.success(fileUrl);

        } catch (Exception e) {
            return Result.fail("图片上传失败：" + e.getMessage());
        }
    }

}