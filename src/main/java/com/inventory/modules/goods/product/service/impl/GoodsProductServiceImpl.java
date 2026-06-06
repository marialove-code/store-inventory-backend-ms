package com.inventory.modules.goods.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.response.Result;
import com.inventory.modules.goods.product.entity.GoodsProduct;
import com.inventory.modules.goods.product.dto.GoodsProductDTO;
import com.inventory.modules.goods.product.vo.GoodsProductListVO;
import com.inventory.modules.goods.product.service.GoodsProductService;
import com.inventory.modules.goods.product.mapper.GoodsProductMapper;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品管理 Service 实现类
 * 功能：商品分页查询、新增、修改、删除、上下架、图片上传
 * 业务关联：商品新增时自动初始化库存记录
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class GoodsProductServiceImpl extends ServiceImpl<GoodsProductMapper, GoodsProduct>
        implements GoodsProductService {

    /**
     * 商品基础 Mapper
     */
    private final GoodsProductMapper goodsProductMapper;

    /**
     * 库存 Mapper（用于商品新增时初始化库存）
     */
    private final InventoryStockMapper inventoryStockMapper;

    /**
     * 商品图片上传物理存储路径
     */
    @Value("${app.upload.product-image-path}")
    private String productImagePath;

    /**
     * 商品图片访问URL前缀
     */
    @Value("${app.upload.product-image-prefix}")
    private String imagePrefix;

    // ======================== 1. 分页查询商品 ========================

    /**
     * 分页 + 多条件组合查询商品列表
     * 支持条件：关键词、分类、品牌、上下架状态、编码、价格区间
     *
     * @param keyword      关键词（名称/编码）
     * @param categoryId   分类ID
     * @param brandId      品牌ID
     * @param shelfStatus  上下架状态
     * @param productCode  商品编码
     * @param minPrice     最低售价
     * @param maxPrice     最高售价
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @return 分页VO数据
     */
    @Override
    public Result<?> pageProduct(String keyword, String categoryId, String brandId, Integer shelfStatus,
                                 String productCode, BigDecimal minPrice, BigDecimal maxPrice,
                                 Long pageNum, Long pageSize) {
        // 初始化分页
        Page<GoodsProductListVO> page = new Page<>(pageNum, pageSize);
        // 直接调用XML联查分页
        Page<GoodsProductListVO> voPage = goodsProductMapper.selectProductWithStock(page,keyword,categoryId,brandId,shelfStatus,productCode,minPrice,maxPrice);
        return Result.success(voPage);
    }

    // ======================== 2. 新增商品 ========================

    /**
     * 新增商品
     * 逻辑：校验编码唯一 → 转换实体 → 保存图片信息 → 插入数据库 → 自动初始化库存
     *
     * @param dto 商品参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addProduct(GoodsProductDTO dto) {
        // 1. 校验商品编码是否重复
        if (StrUtil.isNotBlank(dto.getProductCode())) {
            LambdaQueryWrapper<GoodsProduct> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(GoodsProduct::getProductCode, dto.getProductCode());
            countWrapper.eq(GoodsProduct::getIsDeleted, 0);
            Long count = goodsProductMapper.selectCount(countWrapper);

            if (count > 0) {
                return Result.fail("商品编码已存在，不允许重复添加");
            }
        }

        // 2. 构建商品数据库实体
        GoodsProduct product = new GoodsProduct();

        // 3. 雪花算法生成主键ID
        product.setId(IdUtil.getSnowflakeNextId());

        // 4. 基础商品信息赋值
        product.setProductName(dto.getProductName());
        product.setSpecModel(dto.getSpecModel());
        product.setProductCode(dto.getProductCode());

        // 分类ID：空值判断（替换三元表达式）
        if (StrUtil.isBlank(dto.getCategoryId())) {
            product.setCategoryId(null);
        } else {
            product.setCategoryId(Long.valueOf(dto.getCategoryId()));
        }
        product.setCategoryName(dto.getCategoryName());

        // 品牌ID：空值判断
        if (StrUtil.isBlank(dto.getBrandId())) {
            product.setBrandId(null);
        } else {
            product.setBrandId(Long.valueOf(dto.getBrandId()));
        }
        product.setBrandName(dto.getBrandName());

        product.setSupplierName(dto.getSupplierName());
        product.setManufacturer(dto.getManufacturer());
        product.setUnit(dto.getUnit());

        // 5. 价格信息赋值
        product.setCostPrice(dto.getCostPrice());
        product.setSalePrice(dto.getSalePrice());

        // 6. 库存信息：空值赋默认值0
        if (dto.getStock() == null) {
            product.setStock(0);
        } else {
            product.setStock(dto.getStock());
        }
        if (dto.getStockWarn() == null) {
            product.setStockWarn(0);
        } else {
            product.setStockWarn(dto.getStockWarn());
        }

        // 7. 位置、状态、排序
        product.setShowcasePosition(dto.getShowcasePosition());
        product.setShelfStatus(dto.getShelfStatus());
        if (dto.getSort() == null) {
            product.setSort(0);
        } else {
            product.setSort(dto.getSort());
        }

        // 8. 处理图片列表（List转JSON字符串）
        List<String> images = dto.getImages();
        if (images != null && !images.isEmpty()) {
            product.setImages(JSONUtil.toJsonStr(images));
            // 主图为空则使用第一张
            if (dto.getMainImage() == null) {
                product.setMainImage(images.get(0));
            } else {
                product.setMainImage(dto.getMainImage());
            }
        } else {
            product.setMainImage(dto.getMainImage());
            product.setImages(null);
        }

        // 9. 系统默认字段
        product.setIsDeleted(0);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        // 10. 插入商品数据
        goodsProductMapper.insert(product);

        // 11. 自动初始化库存记录（核心业务）
        InventoryStock stock = new InventoryStock();
        stock.setId(IdUtil.getSnowflakeNextId());
        stock.setGoodsId(product.getId());
        stock.setGoodsName(product.getProductName());
        stock.setCategoryName(product.getCategoryName());

        Integer currentStock = product.getStock();
        Integer warnStock = product.getStockWarn();
        Integer stockStatus;

        if (currentStock > warnStock) {
            // 大于预警值 → 正常 1
            stockStatus = 1;
        } else if (currentStock.equals(warnStock)) {
            // 等于预警值 → 预警 2
            stockStatus = 2;
        } else {
            // 小于预警值 → 缺货 3
            stockStatus = 3;
        }
        stock.setStockStatus(stockStatus);
        stock.setStock(currentStock); //库存
        stock.setLockStock(0);
        stock.setStockWarn(warnStock); //库存预警值
        stock.setSort(0);
        stock.setCreateTime(LocalDateTime.now());
        stock.setUpdateTime(LocalDateTime.now());
        inventoryStockMapper.insert(stock);

        return Result.success("商品新增成功");
    }

    // ======================== 3. 修改商品 ========================

    /**
     * 修改商品信息
     * 逻辑：校验存在 → 校验编码唯一 → 动态更新字段 → 保存
     *
     * @param id  商品ID
     * @param dto 修改参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateProduct(String id, GoodsProductDTO dto) {
        // 1. 转换ID类型
        Long longId = Long.valueOf(id);

        // 2. 校验商品是否存在且未删除
        GoodsProduct oldProduct = goodsProductMapper.selectById(longId);
        if (oldProduct == null) {
            return Result.fail("商品不存在或已删除");
        }
        if (oldProduct.getIsDeleted() == 1) {
            return Result.fail("商品不存在或已删除");
        }

        // 3. 校验商品编码是否重复（排除自身）
        if (StrUtil.isNotBlank(dto.getProductCode())) {
            LambdaQueryWrapper<GoodsProduct> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(GoodsProduct::getProductCode, dto.getProductCode());
            countWrapper.ne(GoodsProduct::getId, longId);
            countWrapper.eq(GoodsProduct::getIsDeleted, 0);
            Long count = goodsProductMapper.selectCount(countWrapper);

            if (count > 0) {
                return Result.fail("商品编码已被其他商品使用");
            }
        }

        // 4. 构建更新条件
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsProduct::getId, longId);

        // 5. 动态设置可更新字段（有值才更新）
        wrapper.set(StrUtil.isNotBlank(dto.getProductName()), GoodsProduct::getProductName, dto.getProductName());
        wrapper.set(StrUtil.isNotBlank(dto.getSpecModel()), GoodsProduct::getSpecModel, dto.getSpecModel());
        wrapper.set(StrUtil.isNotBlank(dto.getCategoryId()), GoodsProduct::getCategoryId, Long.valueOf(dto.getCategoryId()));
        wrapper.set(StrUtil.isNotBlank(dto.getCategoryName()), GoodsProduct::getCategoryName, dto.getCategoryName());
        wrapper.set(StrUtil.isNotBlank(dto.getBrandId()), GoodsProduct::getBrandId, Long.valueOf(dto.getBrandId()));
        wrapper.set(StrUtil.isNotBlank(dto.getBrandName()), GoodsProduct::getBrandName, dto.getBrandName());
        wrapper.set(StrUtil.isNotBlank(dto.getSupplierName()), GoodsProduct::getSupplierName, dto.getSupplierName());
        wrapper.set(StrUtil.isNotBlank(dto.getManufacturer()), GoodsProduct::getManufacturer, dto.getManufacturer());
        wrapper.set(StrUtil.isNotBlank(dto.getUnit()), GoodsProduct::getUnit, dto.getUnit());
        // 价格字段
        wrapper.set(dto.getCostPrice() != null, GoodsProduct::getCostPrice, dto.getCostPrice());
        wrapper.set(dto.getSalePrice() != null, GoodsProduct::getSalePrice, dto.getSalePrice());
        // 库存字段
        wrapper.set(dto.getStock() != null, GoodsProduct::getStock, dto.getStock());
        wrapper.set(dto.getStockWarn() != null, GoodsProduct::getStockWarn, dto.getStockWarn());
        // 位置、状态、排序
        wrapper.set(StrUtil.isNotBlank(dto.getShowcasePosition()), GoodsProduct::getShowcasePosition, dto.getShowcasePosition());
        wrapper.set(dto.getShelfStatus() != null, GoodsProduct::getShelfStatus, dto.getShelfStatus());
        wrapper.set(dto.getSort() != null, GoodsProduct::getSort, dto.getSort());

        // 图片信息
        if (dto.getImages() != null) {
            wrapper.set(GoodsProduct::getImages, JSONUtil.toJsonStr(dto.getImages()));
        }
        if (StrUtil.isNotBlank(dto.getMainImage())) {
            wrapper.set(GoodsProduct::getMainImage, dto.getMainImage());
        }

        // 自动更新时间
        wrapper.set(GoodsProduct::getUpdateTime, LocalDateTime.now());

        // 6. 执行更新
        goodsProductMapper.update(null, wrapper);

        return Result.success("商品修改成功");
    }

    // ======================== 4. 删除商品（逻辑删除） ========================

    /**
     * 单个删除商品（逻辑删除）
     * 校验：有库存则不允许删除
     *
     * @param id 商品ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteProduct(String id) {
        // 1. 转换ID
        Long longId = Long.valueOf(id);

        // 2. 校验商品存在
        GoodsProduct product = goodsProductMapper.selectById(longId);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        if (product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }

        // 3. 查询商品库存（校验库存是否大于0）
        LambdaQueryWrapper<InventoryStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.eq(InventoryStock::getGoodsId, longId);
        InventoryStock stock = inventoryStockMapper.selectOne(stockWrapper);

        // 4. 库存大于0禁止删除
        if (stock != null) {
            if (stock.getStock() > 0) {
                return Result.fail("删除失败：该商品尚有库存，无法删除");
            }
        }

        // 5. 执行逻辑删除
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsProduct::getId, longId);
        wrapper.set(GoodsProduct::getIsDeleted, 1);
        goodsProductMapper.update(null, wrapper);

        return Result.success("商品删除成功");
    }

    // ======================== 5. 批量删除 ========================

    /**
     * 批量逻辑删除商品
     *
     * @param ids ID集合
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchDeleteProduct(List<String> ids) {
        // 1. 空值校验
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的商品");
        }

        // 2. String转Long（替换Stream为for循环）
        List<Long> longIds = new ArrayList<>();
        for (String id : ids) {
            longIds.add(Long.valueOf(id));
        }

        // 3. 构建更新条件
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsProduct::getId, longIds);
        wrapper.set(GoodsProduct::getIsDeleted, 1);

        // 4. 执行批量删除
        goodsProductMapper.update(null, wrapper);

        return Result.success("批量删除成功");
    }

    // ======================== 6. 单个上下架 ========================

    /**
     * 修改商品上下架状态
     *
     * @param id          商品ID
     * @param shelfStatus 0=下架 1=上架
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateShelfStatus(String id, Integer shelfStatus) {
        // 1. 转换ID
        Long longId = Long.valueOf(id);

        // 2. 状态合法性校验
        if (shelfStatus != 0 && shelfStatus != 1) {
            return Result.fail("上下架状态不合法");
        }

        // 3. 校验商品存在
        GoodsProduct product = goodsProductMapper.selectById(longId);
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }

        // 4. 执行状态更新
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsProduct::getId, longId);
        wrapper.set(GoodsProduct::getShelfStatus, shelfStatus);
        goodsProductMapper.update(null, wrapper);

        // 5. 返回提示信息
        String msg;
        if (shelfStatus == 1) {
            msg = "商品上架成功";
        } else {
            msg = "商品下架成功";
        }
        return Result.success(msg);
    }

    // ======================== 7. 批量上下架 ========================

    /**
     * 批量上下架商品
     *
     * @param ids         ID集合
     * @param shelfStatus 状态
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchUpdateShelfStatus(List<String> ids, Integer shelfStatus) {
        // 1. 空值校验
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择商品");
        }
        // 2. 状态校验
        if (shelfStatus != 0 && shelfStatus != 1) {
            return Result.fail("状态不合法");
        }

        // 3. String转Long（普通for循环）
        List<Long> longIds = new ArrayList<>();
        for (String id : ids) {
            longIds.add(Long.valueOf(id));
        }

        // 4. 构建更新条件
        LambdaUpdateWrapper<GoodsProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsProduct::getId, longIds);
        wrapper.set(GoodsProduct::getShelfStatus, shelfStatus);

        // 5. 执行批量更新
        goodsProductMapper.update(null, wrapper);

        return Result.success("批量操作成功");
    }

    // ======================== 8. 图片上传 ========================

    /**
     * 商品图片上传
     * 生成唯一文件名 → 写入磁盘 → 返回访问URL
     *
     * @param file 上传文件
     * @return 图片URL
     */
    @Override
    public Result<?> uploadImage(MultipartFile file) {
        try {
            // 1. 获取原始文件名与后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 2. 生成唯一文件名
            String fileName = IdUtil.getSnowflakeNextId() + suffix;

            // 3. 初始化目录（不存在则创建）
            File dir = new File(productImagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 4. 写入文件
            file.transferTo(new File(dir, fileName));

            // 5. 拼接访问路径并返回
            String fileUrl = imagePrefix + fileName;
            return Result.success(fileUrl);

        } catch (Exception e) {
            // 异常捕获并返回
            return Result.fail("图片上传失败：" + e.getMessage());
        }
    }

}