package com.inventory.modules.invertory.stockout.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.response.Result;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;
import com.inventory.modules.invertory.stockout.mapper.InventoryOutMapper;
import com.inventory.modules.invertory.stockout.service.InventoryOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 出库单业务实现。
 * <p>
 * 适配说明：无登录上下文时操作人固定 {@code system}；
 * 去掉未使用的商品 Mapper 注入。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InventoryOutServiceImpl extends ServiceImpl<InventoryOutMapper, InventoryOut>
        implements InventoryOutService {

    private final InventoryOutMapper stockOutMapper;
    private final StockService stockService;
    private final InventoryStockMapper inventoryStockMapper;

    @Override
    public Result<?> pageStockOut(String outboundNo, String goodsName, String startTime, String endTime,
                                  Long pageNum, Long pageSize) {
        try {
            Page<InventoryOut> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<InventoryOut> wrapper = Wrappers.lambdaQuery();

            if (StrUtil.isNotBlank(outboundNo)) {
                wrapper.eq(InventoryOut::getOutboundNo, outboundNo);
            }
            if (StrUtil.isNotBlank(goodsName)) {
                wrapper.like(InventoryOut::getGoodsName, goodsName);
            }
            if (StrUtil.isNotBlank(startTime)) {
                wrapper.ge(InventoryOut::getCreateTime, startTime);
            }
            if (StrUtil.isNotBlank(endTime)) {
                wrapper.le(InventoryOut::getCreateTime, endTime);
            }
            wrapper.orderByDesc(InventoryOut::getCreateTime);

            return Result.success(stockOutMapper.selectPage(page, wrapper));
        } catch (Exception e) {
            return Result.fail("出库单分页查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getStockOutDetail(Long id) {
        try {
            InventoryOut inventoryOut = stockOutMapper.selectById(id);
            if (inventoryOut == null) {
                return Result.fail("出库单不存在");
            }
            return Result.success(inventoryOut);
        } catch (Exception e) {
            return Result.fail("查询出库单详情失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addStockOut(StockOutAddDTO dto) {
        try {
            Long goodsId;
            try {
                goodsId = Long.valueOf(dto.getGoodsId());
            } catch (NumberFormatException e) {
                return Result.fail("商品ID格式错误，必须为数字");
            }

            // 出库前校验可用库存 = 总库存 - 锁定库存
            LambdaQueryWrapper<InventoryStock> stockQueryWrapper = new LambdaQueryWrapper<>();
            stockQueryWrapper.eq(InventoryStock::getGoodsId, goodsId);
            InventoryStock stock = inventoryStockMapper.selectOne(stockQueryWrapper);
            if (stock == null) {
                return Result.fail("商品库存记录不存在");
            }
            int usableStock = stock.getStock() - stock.getLockStock();
            if (usableStock < dto.getOutboundQty()) {
                return Result.fail("库存不足，当前可用库存：" + usableStock);
            }

            InventoryOut inventoryOut = new InventoryOut();
            inventoryOut.setGoodsId(goodsId);
            inventoryOut.setGoodsName(dto.getGoodsName());
            inventoryOut.setOutboundQty(dto.getOutboundQty());
            inventoryOut.setRemark(dto.getRemark());
            inventoryOut.setCreateTime(LocalDateTime.now());

            Integer maxSort = stockOutMapper.selectMaxSort();
            inventoryOut.setSort(maxSort == null ? 1 : maxSort + 1);

            String outboundNo = OrderNoGenerator.generate(OrderPrefix.OUTBOUND);
            inventoryOut.setOutboundNo(outboundNo);
            inventoryOut.setOperator("system");

            stockOutMapper.insert(inventoryOut);
            // 同库事务：出库单 + 扣减可用库存 + 流水
            stockService.decreaseStock(goodsId, dto.getOutboundQty());
            return Result.success("新增出库单成功，出库单号：" + outboundNo);
        } catch (Exception e) {
            return Result.fail("新增出库单失败：" + e.getMessage());
        }
    }
}
