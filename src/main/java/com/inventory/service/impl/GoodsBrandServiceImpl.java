package com.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.result.Result;
import com.inventory.entity.goods.*;
import com.inventory.mapper.GoodsCategoryMapper;
import com.inventory.service.GoodsBrandService;
import com.inventory.mapper.GoodsBrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author 95349
 * @description 针对表【goods_brand(商品品牌表)】的数据库操作Service实现
 * @createDate 2026-05-26 19:13:00
 * 商品分类 Service 实现
 */
@Service
@RequiredArgsConstructor
public class GoodsBrandServiceImpl extends ServiceImpl<GoodsBrandMapper, GoodsBrand>
    implements GoodsBrandService{

    private final GoodsBrandMapper goodsBrandMapper;

    @Value("${app.upload.brand-image-path}")
    private String uploadPath;

    @Value("${app.upload.brand-image-prefix}")
    private String accessUrl;

    // ====================== 分页查询 ======================
    @Override
    public Result<?> page(String keyword, Long pageNum, Long pageSize) {
        Page<GoodsBrand> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<GoodsBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsBrand::getIsDeleted, 0);

        // 名称 + 编码 模糊搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(GoodsBrand::getBrandName, keyword)
                    .or().like(GoodsBrand::getBrandCode, keyword));
        }

        wrapper.orderByAsc(GoodsBrand::getSort);
        wrapper.orderByDesc(GoodsBrand::getCreateTime);

        Page<GoodsBrand> brandPage = this.page(page, wrapper);

        Page<GoodsBrandVO> voPage = new Page<>(
                brandPage.getCurrent(),
                brandPage.getSize(),
                brandPage.getTotal()
        );

        List<GoodsBrandVO> voList = brandPage.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, GoodsBrandVO.class))
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    // ====================== 全部品牌（下拉） ======================
    @Override
    public Result<?> listAll() {
        LambdaQueryWrapper<GoodsBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsBrand::getIsDeleted, 0);
        wrapper.eq(GoodsBrand::getStatus, 1);
        wrapper.orderByAsc(GoodsBrand::getSort);

        List<GoodsBrand> list = this.list(wrapper);
        List<GoodsBrandVO> voList = list.stream()
                .map(item -> BeanUtil.copyProperties(item, GoodsBrandVO.class))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    // ====================== 新增 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> add(GoodsBrandDTO dto) {
        GoodsBrand brand = new GoodsBrand();
        BeanUtil.copyProperties(dto, brand);

        brand.setIsDeleted(0);
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());

        this.save(brand);
        return Result.success("新增成功");
    }

    // ====================== 修改 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(String id, GoodsBrandDTO dto) {
        Long longId = Long.valueOf(id);
        GoodsBrand brand = this.getById(longId);
        if (brand == null || brand.getIsDeleted() == 1) {
            return Result.fail("品牌不存在或已删除");
        }

        BeanUtil.copyProperties(dto, brand);
        brand.setUpdateTime(LocalDateTime.now());

        this.updateById(brand);
        return Result.success("修改成功");
    }

    // ====================== 删除 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(String id) {
        Long longId = Long.valueOf(id);
        LambdaUpdateWrapper<GoodsBrand> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsBrand::getId, longId);
        wrapper.set(GoodsBrand::getIsDeleted, 1);
        wrapper.set(GoodsBrand::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("删除成功");
    }

    // ====================== 批量删除 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchDelete(List<String> ids) {
        List<Long> longIds = ids.stream().map(Long::valueOf).collect(Collectors.toList());

        LambdaUpdateWrapper<GoodsBrand> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsBrand::getId, longIds);
        wrapper.set(GoodsBrand::getIsDeleted, 1);
        wrapper.set(GoodsBrand::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("批量删除成功");
    }

    // ====================== 状态修改 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStatus(String id, Integer status) {
        Long longId = Long.valueOf(id);

        LambdaUpdateWrapper<GoodsBrand> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsBrand::getId, longId);
        wrapper.set(GoodsBrand::getStatus, status);
        wrapper.set(GoodsBrand::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("状态修改成功");
    }

    // ====================== LOGO上传 ======================
    @Override
    public Result<?> uploadLogo(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + suffix;

            File dir = new File(uploadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(dir, fileName);
            file.transferTo(dest);

            String url = accessUrl + fileName;
            return Result.success(url);
        } catch (Exception e) {
            return Result.fail("上传失败：" + e.getMessage());
        }
    }

}




