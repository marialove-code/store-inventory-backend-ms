package com.inventory.modules.invertory.stockin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.response.Result;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.entity.InventoryIn;
import com.inventory.modules.invertory.stockin.mapper.InventoryInMapper;
import com.inventory.modules.invertory.stockin.service.InventoryInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 入库单业务实现。
 * <p>
 * 适配说明：微服务阶段无登录上下文，操作人固定为 {@code system}；
 * 去掉对商品 Mapper 的无用注入。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InventoryInServiceImpl extends ServiceImpl<InventoryInMapper, InventoryIn>
        implements InventoryInService {

    private final InventoryInMapper stockInMapper;
    private final StockService stockService;

    @Override
    public Result<?> pageStockIn(String receiptNo, String goodsName, String startTime, String endTime,
                                 Long pageNum, Long pageSize) {
        try {
            Page<InventoryIn> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<InventoryIn> wrapper = Wrappers.lambdaQuery();

            if (StrUtil.isNotBlank(receiptNo)) {
                wrapper.eq(InventoryIn::getReceiptNo, receiptNo);
            }
            if (StrUtil.isNotBlank(goodsName)) {
                wrapper.like(InventoryIn::getGoodsName, goodsName);
            }
            if (StrUtil.isNotBlank(startTime)) {
                wrapper.ge(InventoryIn::getCreateTime, startTime);
            }
            if (StrUtil.isNotBlank(endTime)) {
                wrapper.le(InventoryIn::getCreateTime, endTime);
            }
            wrapper.orderByDesc(InventoryIn::getCreateTime);

            return Result.success(stockInMapper.selectPage(page, wrapper));
        } catch (Exception e) {
            return Result.fail("入库单分页查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getStockInDetail(Long id) {
        try {
            InventoryIn inventoryIn = stockInMapper.selectById(id);
            if (inventoryIn == null) {
                return Result.fail("入库单不存在");
            }
            return Result.success(inventoryIn);
        } catch (Exception e) {
            return Result.fail("查询入库单详情失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addStockIn(StockInAddDTO dto) {
        try {
            Long goodsId;
            try {
                goodsId = Long.valueOf(dto.getGoodsId());
            } catch (NumberFormatException e) {
                return Result.fail("商品ID格式错误，必须为数字");
            }

            InventoryIn inventoryIn = new InventoryIn();
            inventoryIn.setGoodsId(goodsId);
            inventoryIn.setGoodsName(dto.getGoodsName());
            inventoryIn.setReceiptQty(dto.getReceiptQty());
            inventoryIn.setRemark(dto.getRemark());
            inventoryIn.setCreateTime(LocalDateTime.now());

            // 生成入库单号，并作为库存流水 bizNo
            String receiptNo = OrderNoGenerator.generate(OrderPrefix.INBOUND);
            inventoryIn.setReceiptNo(receiptNo);
            // 本阶段无鉴权，操作人固定 system（与 StockService 写流水一致）
            inventoryIn.setOperator("system");

            stockInMapper.insert(inventoryIn);
            // 同库事务内：入库单 + 库存增加 + 流水
            stockService.increaseStockFlow(goodsId, dto.getReceiptQty(), receiptNo);
            return Result.success("新增入库单成功，入库单号：" + receiptNo);
        } catch (Exception e) {
            return Result.fail("新增入库单失败：" + e.getMessage());
        }
    }
}
