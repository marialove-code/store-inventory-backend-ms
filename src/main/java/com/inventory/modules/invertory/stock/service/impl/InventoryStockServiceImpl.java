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

import java.util.ArrayList;
import java.util.List;

/**
 * 库存实时信息 Service 实现类
 * 功能：库存分页查询、库存预警值修改
 * 对应表：inventory_stock
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl extends ServiceImpl<InventoryStockMapper, InventoryStock>
        implements InventoryStockService {

    /**
     * 库存实时表 Mapper
     */
    private final InventoryStockMapper stockMapper;

    // ======================== 1. 库存分页列表查询 ========================
    /**
     * 库存分页多条件查询
     * 支持条件：商品名称、分类名称、库存状态
     * 返回：库存列表VO分页数据
     *
     * @param goodsName     商品名称（模糊）
     * @param categoryName 分类名称（模糊）
     * @param stockStatus   库存状态 1正常 2预警 3缺货
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @return 分页数据
     */
    @Override
    public Result<?> pageStock(String goodsName, String categoryName, Integer stockStatus,
                               Long pageNum, Long pageSize) {
        // 1. 初始化 MyBatis-Plus 分页对象
        Page<InventoryStock> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件构造器
        LambdaQueryWrapper<InventoryStock> wrapper = Wrappers.lambdaQuery();

        // 3. 商品名称模糊匹配
        if (StrUtil.isNotBlank(goodsName)) {
            wrapper.like(InventoryStock::getGoodsName, goodsName);
        }

        // 4. 分类名称模糊匹配
        if (StrUtil.isNotBlank(categoryName)) {
            wrapper.like(InventoryStock::getCategoryName, categoryName);
        }

        // 5. 库存状态精确匹配
        if (stockStatus != null) {
            wrapper.eq(InventoryStock::getStockStatus, stockStatus);
        }

        // 6. 排序规则：按 sort 升序，创建时间降序
        wrapper.orderByAsc(InventoryStock::getSort);
        wrapper.orderByDesc(InventoryStock::getCreateTime);

        // 7. 执行分页查询
        Page<InventoryStock> stockPage = stockMapper.selectPage(page, wrapper);

        // 8. 构建 VO 分页对象（保持分页信息不变）
        Page<StockListVO> voPage = new Page<>(
                stockPage.getCurrent(),
                stockPage.getSize(),
                stockPage.getTotal()
        );

        // 9. 实体列表转换为 VO 列表（替换 Stream 为普通 for 循环）
        List<InventoryStock> records = stockPage.getRecords();
        List<StockListVO> voList = new ArrayList<>();

        for (InventoryStock stock : records) {
            StockListVO vo = new StockListVO();
            // 属性拷贝：保持原逻辑不变
            BeanUtil.copyProperties(stock, vo);
            voList.add(vo);
        }

        // 10. 设置转换后的 VO 列表到分页对象
        voPage.setRecords(voList);

        // 11. 返回成功结果
        return Result.success(voPage);
    }

    // ======================== 2. 修改库存预警阈值 ========================
    /**
     * 修改商品库存预警值
     * 逻辑：校验库存记录是否存在 → 更新 stockWarn 字段
     *
     * @param id  库存记录ID
     * @param dto 预警值参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStockWarn(String id, StockWarnDTO dto) {
        // 1. 转换ID类型 String → Long
        Long longId = Long.valueOf(id);

        // 2. 根据ID查询库存记录
        InventoryStock stock = getById(longId);

        // 3. 校验库存记录是否存在
        if (stock == null) {
            return Result.fail("库存数据不存在");
        }

        // 4. 设置新的库存预警值
        stock.setStockWarn(dto.getStockWarn());

        // 5. 执行更新操作
        updateById(stock);

        // 6. 返回成功提示
        return Result.success("修改成功");
    }
}