package com.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.result.Result;
import com.inventory.entity.goods.GoodsCategory;
import com.inventory.entity.goods.GoodsCategoryDTO;
import com.inventory.entity.goods.GoodsCategoryVO;
import com.inventory.service.GoodsCategoryService;
import com.inventory.mapper.GoodsCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【goods_category(商品分类表)】的数据库操作Service实现
* @createDate 2026-05-26 19:13:00
*/
@Service
@RequiredArgsConstructor
public class GoodsCategoryServiceImpl extends ServiceImpl<GoodsCategoryMapper, GoodsCategory>
    implements GoodsCategoryService{

    private final GoodsCategoryMapper goodsCategoryMapper;

    // ====================== 分页查询 ======================
    @Override
    public Result<?> page(String keyword, Long pageNum, Long pageSize) {
        Page<GoodsCategory> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<GoodsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsCategory::getIsDeleted, 0);

        // 分类名称模糊搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(GoodsCategory::getCategoryName, keyword);
        }

        // 排序：sort 正序 + 时间倒序
        wrapper.orderByAsc(GoodsCategory::getSort);
        wrapper.orderByDesc(GoodsCategory::getCreateTime);

        Page<GoodsCategory> categoryPage = this.page(page, wrapper);

        // 转 VO
        Page<GoodsCategoryVO> voPage = new Page<>(
                categoryPage.getCurrent(),
                categoryPage.getSize(),
                categoryPage.getTotal()
        );

        List<GoodsCategoryVO> voList = categoryPage.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, GoodsCategoryVO.class))
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    // ====================== 分类树（最常用） ======================
    @Override
    public Result<?> tree(String keyword) {
        LambdaQueryWrapper<GoodsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsCategory::getIsDeleted, 0);
        wrapper.eq(GoodsCategory::getStatus, 1); // 只查启用的

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(GoodsCategory::getCategoryName, keyword);
        }

        wrapper.orderByAsc(GoodsCategory::getSort);

        List<GoodsCategory> allList = this.list(wrapper);
        List<GoodsCategoryVO> voList = allList.stream()
                .map(item -> BeanUtil.copyProperties(item, GoodsCategoryVO.class))
                .collect(Collectors.toList());

        // 构建树结构（顶级 parentId = 0）
        List<GoodsCategoryVO> tree = buildTree(voList, "0");
        return Result.success(tree);
    }

    /**
     * 递归构建分类树
     */
    private List<GoodsCategoryVO> buildTree(List<GoodsCategoryVO> list, String parentId) {
        return list.stream()
                .filter(vo -> String.valueOf(vo.getParentId()).equals(parentId))
                .peek(vo -> vo.setChildren(buildTree(list, String.valueOf(vo.getId()))))
                .collect(Collectors.toList());
    }

    // ====================== 新增 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> add(GoodsCategoryDTO dto) {
        GoodsCategory category = new GoodsCategory();
        BeanUtil.copyProperties(dto, category);

        // String 转 Long
        category.setParentId(Long.valueOf(dto.getParentId()));
        category.setIsDeleted(0);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        this.save(category);
        return Result.success("新增成功");
    }

    // ====================== 修改 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(String id, GoodsCategoryDTO dto) {
        Long longId = Long.valueOf(id);
        GoodsCategory category = this.getById(longId);
        if (category == null || category.getIsDeleted() == 1) {
            return Result.fail("分类不存在或已删除");
        }

        BeanUtil.copyProperties(dto, category);
        category.setParentId(Long.valueOf(dto.getParentId()));
        category.setUpdateTime(LocalDateTime.now());

        this.updateById(category);
        return Result.success("修改成功");
    }

    // ====================== 删除 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(String id) {
        Long longId = Long.valueOf(id);
        LambdaUpdateWrapper<GoodsCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsCategory::getId, longId);
        wrapper.set(GoodsCategory::getIsDeleted, 1);
        wrapper.set(GoodsCategory::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("删除成功");
    }

    // ====================== 批量删除 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchDelete(List<String> ids) {
        List<Long> longIds = ids.stream().map(Long::valueOf).collect(Collectors.toList());

        LambdaUpdateWrapper<GoodsCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsCategory::getId, longIds);
        wrapper.set(GoodsCategory::getIsDeleted, 1);
        wrapper.set(GoodsCategory::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("批量删除成功");
    }

    // ====================== 状态修改 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStatus(String id, Integer status) {
        Long longId = Long.valueOf(id);

        LambdaUpdateWrapper<GoodsCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GoodsCategory::getId, longId);
        wrapper.set(GoodsCategory::getStatus, status);
        wrapper.set(GoodsCategory::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("状态修改成功");
    }

    // ====================== 批量状态修改 ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchUpdateStatus(List<String> ids, Integer status) {
        List<Long> longIds = ids.stream().map(Long::valueOf).collect(Collectors.toList());

        LambdaUpdateWrapper<GoodsCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(GoodsCategory::getId, longIds);
        wrapper.set(GoodsCategory::getStatus, status);
        wrapper.set(GoodsCategory::getUpdateTime, LocalDateTime.now());

        this.update(wrapper);
        return Result.success("批量状态修改成功");
    }
}




