package com.inventory.modules.invertory.stockwarn.impl;


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
* @author 95349
* @createDate 2026-05-29 19:04:18
*/
@Service
@RequiredArgsConstructor
public class InventoryWarnServiceImpl implements InventoryWarnService {

    /**
     * 注入库存Mapper
     */
    private final InventoryStockMapper stockMapper;

    /**
     * 库存预警分页查询
     * 条件：库存数量 < 预警阈值，即为预警商品
     */
    @Override
    public Result<?> pageWarnList(String goodsName, Long pageNum, Long pageSize) {
        // 1. 构建分页对象
        Page<InventoryStock> page = new Page<>(pageNum, pageSize);

        // 2. 构造查询条件
        LambdaQueryWrapper<InventoryStock> wrapper = Wrappers.lambdaQuery();


        // 商品名称模糊查询
        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryStock::getGoodsName, goodsName);
        }

        // ✅ 修复后的核心条件：库存数量 < 预警阈值 → 预警数据
        wrapper.apply("stock < stock_warn");


        // 排序：库存少的排前面
        wrapper.orderByAsc(InventoryStock::getStock);

        // 3. 执行分页查询
        Page<InventoryStock> stockPage = stockMapper.selectPage(page, wrapper);

        return Result.success(stockPage);
    }

    /**
     * 获取预警商品详情
     * 根据ID查询单条库存记录
     */
    @Override
    public Result<?> getWarnDetail(String id) {
        InventoryStock stock = stockMapper.selectById(Long.valueOf(id));
        if (stock == null) {
            return Result.fail("库存记录不存在");
        }
        return Result.success(stock);
    }

    /**
     * 修改商品预警阈值
     * 事务控制：保证数据一致性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStockWarn(String id, StockWarnDTO dto) {
        // 1. 查询原库存数据
        InventoryStock stock = stockMapper.selectById(Long.valueOf(id));
        if (stock == null) {
            return Result.fail("库存记录不存在");
        }

        // 2. 更新预警阈值
        stock.setStockWarn(dto.getStockWarn());
        stockMapper.updateById(stock);

        return Result.success("预警阈值修改成功");
    }

}




