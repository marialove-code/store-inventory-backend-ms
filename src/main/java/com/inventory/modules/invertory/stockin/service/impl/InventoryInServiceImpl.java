package com.inventory.modules.invertory.stockin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.response.Result;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.entity.InventoryIn;
import com.inventory.modules.invertory.stockin.service.InventoryInService;
import com.inventory.modules.invertory.stockin.mapper.InventoryInMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
* @author 95349
* @description 针对表【inventory_in】的数据库操作Service实现
* @createDate 2026-05-29 19:04:18
*/
@Service
@RequiredArgsConstructor
public class InventoryInServiceImpl extends ServiceImpl<InventoryInMapper, InventoryIn>
    implements InventoryInService{

    private final InventoryInMapper stockInMapper;

    /**
     * 入库单分页查询
     */
    @Override
    public Result<?> pageStockIn(String receiptNo, String goodsName, String startTime, String endTime, Long pageNum, Long pageSize) {
        // 构建分页对象
        Page<InventoryIn> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<InventoryIn> wrapper = Wrappers.lambdaQuery();

        // 入库单号 精准匹配
        if (StrUtil.isNotBlank(receiptNo)) {
            wrapper.eq(InventoryIn::getReceiptNo, receiptNo);
        }

        // 商品名称 模糊查询
        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryIn::getGoodsName, goodsName);
        }

        // 时间范围查询
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(InventoryIn::getCreateTime, startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(InventoryIn::getCreateTime, endTime);
        }

        // 排序：创建时间倒序
        wrapper.orderByDesc(InventoryIn::getCreateTime);

        // 执行查询
        Page<InventoryIn> resultPage = stockInMapper.selectPage(page, wrapper);

        return Result.success(resultPage);
    }

    /**
     * 查询入库单详情
     */
    @Override
    public Result<?> getStockInDetail(Long id) {
        InventoryIn inventoryIn = stockInMapper.selectById(id);
        if (inventoryIn == null) {
            return Result.fail("入库单不存在");
        }
        return Result.success(inventoryIn);
    }

    /**
     * 新增入库单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addStockIn(StockInAddDTO dto) {
        // 构建实体
        InventoryIn inventoryIn = new InventoryIn();

        // 复制 DTO 字段
        inventoryIn.setGoodsId(Long.valueOf(dto.getGoodsId()));
        inventoryIn.setGoodsName(dto.getGoodsName());
        inventoryIn.setReceiptQty(dto.getReceiptQty());
        inventoryIn.setRemark(dto.getRemark());
        // 补充系统字段
        inventoryIn.setCreateTime(LocalDateTime.now());
        String generate = OrderNoGenerator.generate(OrderPrefix.INBOUND);
        inventoryIn.setReceiptNo(generate);
        LoginUserVO user = LoginUserContext.getUser();
        inventoryIn.setOperator(user.getUsername());
        // 保存
        stockInMapper.insert(inventoryIn);
        return Result.success("新增入库单成功");
    }

}




