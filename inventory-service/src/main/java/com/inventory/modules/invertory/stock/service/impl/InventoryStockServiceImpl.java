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
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.InventoryStockService;
import com.inventory.modules.invertory.stock.vo.StockListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 库存实时信息 Service 实现。
 * <p>
 * 功能：库存分页查询、库存预警值修改。
 * 适配说明：单体中曾注入 {@code GoodsProductMapper} 但未实际使用，微服务阶段已去掉该依赖，
 * 避免牵连商品模块。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl extends ServiceImpl<InventoryStockMapper, InventoryStock>
        implements InventoryStockService {

    private final InventoryStockMapper stockMapper;

    /**
     * 库存分页多条件查询。
     * 支持：商品名称、分类名称、库存状态；返回 VO 分页数据。
     */
    @Override
    public Result<?> pageStock(String goodsName, String categoryName, Integer stockStatus,
                               Long pageNum, Long pageSize) {
        Page<InventoryStock> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<InventoryStock> wrapper = Wrappers.lambdaQuery();

        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryStock::getGoodsName, goodsName);
        }
        if (StrUtil.isNotBlank(categoryName)) {
            wrapper.like(InventoryStock::getCategoryName, categoryName);
        }
        if (stockStatus != null) {
            wrapper.eq(InventoryStock::getStockStatus, stockStatus);
        }

        wrapper.orderByAsc(InventoryStock::getSort);
        wrapper.orderByDesc(InventoryStock::getCreateTime);

        Page<InventoryStock> stockPage = stockMapper.selectPage(page, wrapper);

        Page<StockListVO> voPage = new Page<>(
                stockPage.getCurrent(),
                stockPage.getSize(),
                stockPage.getTotal()
        );

        List<InventoryStock> records = stockPage.getRecords();
        List<StockListVO> voList = new ArrayList<>();
        for (InventoryStock stock : records) {
            StockListVO vo = new StockListVO();
            BeanUtil.copyProperties(stock, vo);
            voList.add(vo);
        }
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    /**
     * 修改商品库存预警值（及可用库存字段）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStockWarn(String id, StockWarnDTO dto) {
        Long longId = Long.valueOf(id);
        InventoryStock stock = getById(longId);
        if (stock == null) {
            return Result.fail("库存数据不存在");
        }

        stock.setStockWarn(dto.getStockWarn());
        stock.setStock(dto.getStock());
        updateById(stock);

        return Result.success("修改成功");
    }
}
