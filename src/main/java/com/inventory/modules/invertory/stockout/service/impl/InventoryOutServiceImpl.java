package com.inventory.modules.invertory.stockout.service.impl;

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
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;
import com.inventory.modules.invertory.stockout.service.InventoryOutService;
import com.inventory.modules.invertory.stockout.mapper.InventoryOutMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
* @author 95349
* @description 针对表【inventory_out】的数据库操作Service实现
* @createDate 2026-05-29 19:04:18
*/
@Service
@RequiredArgsConstructor
public class InventoryOutServiceImpl extends ServiceImpl<InventoryOutMapper, InventoryOut>
    implements InventoryOutService{


    private final InventoryOutMapper stockOutMapper;

    /**
     * 出库单分页查询
     */
    @Override
    public Result<?> pageStockOut(String outboundNo, String goodsName, String startTime, String endTime, Long pageNum, Long pageSize) {
        // 构建分页对象
        Page<InventoryOut> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<InventoryOut> wrapper = Wrappers.lambdaQuery();

        // 出库单号 精准匹配
        if (StrUtil.isNotBlank(outboundNo)) {
            wrapper.eq(InventoryOut::getOutboundNo, outboundNo);
        }

        // 商品名称 模糊查询
        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryOut::getGoodsName, goodsName);
        }

        // 时间范围查询
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(InventoryOut::getCreateTime, startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(InventoryOut::getCreateTime, endTime);
        }

        // 排序：创建时间倒序
        wrapper.orderByDesc(InventoryOut::getCreateTime);

        // 执行查询
        Page<InventoryOut> resultPage = stockOutMapper.selectPage(page, wrapper);

        return Result.success(resultPage);
    }

    /**
     * 查询出库单详情
     */
    @Override
    public Result<?> getStockOutDetail(Long id) {
        InventoryOut inventoryOut = stockOutMapper.selectById(id);
        if (inventoryOut == null) {
            return Result.fail("出库单不存在");
        }
        return Result.success(inventoryOut);
    }

    /**
     * 新增出库单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addStockOut(StockOutAddDTO dto) {
        // 构建实体
        InventoryOut inventoryOut = new InventoryOut();

        // 复制 DTO 字段
        inventoryOut.setGoodsId(dto.getGoodsId() != null ? Long.valueOf(dto.getGoodsId()) : null);
        inventoryOut.setGoodsName(dto.getGoodsName());
        inventoryOut.setOutboundQty(dto.getOutboundQty());
        inventoryOut.setRemark(dto.getRemark());

        // 补充系统字段
        inventoryOut.setCreateTime(LocalDateTime.now());
        Integer maxSort = stockOutMapper.selectMaxSort();

        inventoryOut.setSort(maxSort + 1);
        String generate = OrderNoGenerator.generate(OrderPrefix.OUTBOUND);
        inventoryOut.setOutboundNo(generate);
        LoginUserVO user = LoginUserContext.getUser();
        inventoryOut.setOperator(user.getUsername());
        // 保存
        stockOutMapper.insert(inventoryOut);

        return Result.success("新增出库单成功");
    }

}




