package com.inventory.modules.invertory.stockwarn.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stockwarn.dto.StockWarnDTO;
import com.inventory.modules.invertory.stockwarn.service.InventoryWarnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存预警业务实现：筛选库存低于预警阈值的商品，并支持改阈值。
 */
@Service
@RequiredArgsConstructor
public class InventoryWarnServiceImpl implements InventoryWarnService {

    private final InventoryStockMapper stockMapper;

    @Override
    public Result<?> pageWarnList(String goodsName, Long pageNum, Long pageSize) {
        Page<InventoryStock> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InventoryStock> wrapper = Wrappers.lambdaQuery();

        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryStock::getGoodsName, goodsName);
        }
        // 库存数量小于预警阈值 → 进入预警列表
        wrapper.apply("stock < stock_warn");
        wrapper.orderByAsc(InventoryStock::getStock);

        return Result.success(stockMapper.selectPage(page, wrapper));
    }

    @Override
    public Result<?> getWarnDetail(String id) {
        InventoryStock stock = stockMapper.selectById(Long.valueOf(id));
        if (stock == null) {
            return Result.fail("库存记录不存在");
        }
        return Result.success(stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStockWarn(String id, StockWarnDTO dto) {
        InventoryStock stock = stockMapper.selectById(Long.valueOf(id));
        if (stock == null) {
            return Result.fail("库存记录不存在");
        }
        stock.setStockWarn(dto.getStockWarn());
        stockMapper.updateById(stock);
        return Result.success("预警阈值修改成功");
    }
}
