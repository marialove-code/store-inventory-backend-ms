package com.inventory.modules.invertory.stock.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.dto.StockWarnDTO;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.service.InventoryStockService;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.vo.StockListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【inventory_stock】的数据库操作Service实现
* @createDate 2026-05-29 19:04:18
*/
@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl extends ServiceImpl<InventoryStockMapper, InventoryStock>
    implements InventoryStockService{

    private final InventoryStockMapper stockMapper;

    /**
     * 库存分页列表查询
     * 筛选条件：商品名称、商品分类名称、库存状态
     */
    @Override
    public Result<?> pageStock(String goodsName, String categoryName, Integer stockStatus,
                               Long pageNum, Long pageSize) {

        // 1. 构建分页对象
        Page<InventoryStock> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件（只查未删除）
        LambdaQueryWrapper<InventoryStock> wrapper = Wrappers.lambdaQuery();

        // 商品名称模糊查询
        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryStock::getGoodsName, goodsName);
        }

        // 商品分类名称（中文）精确/模糊查询
        if (StrUtil.isNotBlank(categoryName)) {
            wrapper.like(InventoryStock::getCategoryName, categoryName);
        }

        // 库存状态
        if (stockStatus != null) {
            wrapper.eq(InventoryStock::getStockStatus, stockStatus);
        }

        // 排序：sort正序 + 创建时间倒序
        wrapper.orderByAsc(InventoryStock::getSort);
        wrapper.orderByDesc(InventoryStock::getCreateTime);

        // 3. 执行分页查询
        Page<InventoryStock> stockPage = stockMapper.selectPage(page, wrapper);

        // 4. 转换为VO分页
        Page<StockListVO> voPage = new Page<>(
                stockPage.getCurrent(),
                stockPage.getSize(),
                stockPage.getTotal()
        );

        List<StockListVO> voList = stockPage.getRecords().stream()
                .map(stock -> {
                    StockListVO vo = new StockListVO();
                    BeanUtil.copyProperties(stock, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    /**
     * 修改库存预警阈值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStockWarn(String id, StockWarnDTO dto) {
        InventoryStock stock = getById(Long.valueOf(id));
        if (stock == null) {
            return Result.fail("库存数据不存在");
        }

        // 更新预警阈值
        stock.setStockWarn(dto.getStockWarn());
        updateById(stock);

        return Result.success("修改成功");
    }
}




