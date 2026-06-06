package com.inventory.modules.invertory.stockout.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.constants.OrderPrefix;
import com.inventory.common.response.Result;
import com.inventory.common.utils.OrderNoGenerator;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.goods.product.entity.GoodsProduct;
import com.inventory.modules.goods.product.mapper.GoodsProductMapper;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;
import com.inventory.modules.invertory.stockout.service.InventoryOutService;
import com.inventory.modules.invertory.stockout.mapper.InventoryOutMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 出库单管理 Service 实现类
 *
 * 功能说明：
 * 1. 出库单分页查询、详情查询
 * 2. 新增出库单（库存充足校验 + 生成单号 + 扣减库存）
 * 3. 事务保证：出库单创建与库存扣减 原子性操作
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class InventoryOutServiceImpl extends ServiceImpl<InventoryOutMapper, InventoryOut>
        implements InventoryOutService {

    /**
     * 出库单 Mapper
     */
    private final InventoryOutMapper stockOutMapper;

    /**
     * 库存核心服务（扣减库存、写流水、刷新状态）
     */
    private final StockService stockService;

    /**
     * 库存实时表 Mapper（用于库存充足性校验）
     */
    private final InventoryStockMapper inventoryStockMapper;

    /**
     * 商品基础 Mapper
     */
    private final GoodsProductMapper goodsProductMapper;

    // ======================== 1. 出库单分页查询 ========================
    /**
     * 出库单分页多条件查询
     * 支持条件：出库单号、商品名称、创建时间区间
     * 排序规则：创建时间倒序，最新数据优先
     */
    @Override
    public Result<?> pageStockOut(String outboundNo, String goodsName, String startTime, String endTime,
                                  Long pageNum, Long pageSize) {
        try {
            // 1. 初始化分页对象
            Page<InventoryOut> page = new Page<>(pageNum, pageSize);

            // 2. 构建查询条件
            LambdaQueryWrapper<InventoryOut> wrapper = Wrappers.lambdaQuery();

            // 3. 出库单号精确匹配
            if (StrUtil.isNotBlank(outboundNo)) {
                wrapper.eq(InventoryOut::getOutboundNo, outboundNo);
            }

            // 4. 商品名称模糊匹配
            if (StrUtil.isNotBlank(goodsName)) {
                wrapper.like(InventoryOut::getGoodsName, goodsName);
            }

            // 5. 创建时间 >= 开始时间
            if (StrUtil.isNotBlank(startTime)) {
                wrapper.ge(InventoryOut::getCreateTime, startTime);
            }

            // 6. 创建时间 <= 结束时间
            if (StrUtil.isNotBlank(endTime)) {
                wrapper.le(InventoryOut::getCreateTime, endTime);
            }

            // 7. 排序：创建时间倒序
            wrapper.orderByDesc(InventoryOut::getCreateTime);

            // 8. 执行分页查询
            Page<InventoryOut> resultPage = stockOutMapper.selectPage(page, wrapper);

            // 9. 返回成功结果
            return Result.success(resultPage);

        } catch (Exception e) {
            // 10. 异常捕获并返回友好提示
            return Result.fail("出库单分页查询失败：" + e.getMessage());
        }
    }


    // ======================== 2. 新增出库单 + 库存扣减 ========================
    /**
     * 新增出库单（核心业务）
     * 逻辑：
     * 1. 商品ID格式校验
     * 2. 库存充足性校验（可用库存 = 总库存 - 锁定库存）
     * 3. 生成唯一出库单号
     * 4. 填充操作人、创建时间、排序号
     * 5. 保存出库单
     * 6. 调用库存服务扣减可用库存（事务保证）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addStockOut(StockOutAddDTO dto) {
        try {
            // 1. 商品ID格式转换与校验
            Long goodsId;
            try {
                goodsId = Long.valueOf(dto.getGoodsId());
            } catch (NumberFormatException e) {
                return Result.fail("商品ID格式错误，必须为数字");
            }

            // ===================== 核心：出库前库存充足性校验 =====================
            // 2. 查询商品库存记录
            LambdaQueryWrapper<InventoryStock> stockQueryWrapper = new LambdaQueryWrapper<>();
            stockQueryWrapper.eq(InventoryStock::getGoodsId, goodsId);
            InventoryStock stock = inventoryStockMapper.selectOne(stockQueryWrapper);

            // 3. 库存记录不存在判断
            if (stock == null) {
                return Result.fail("商品库存记录不存在");
            }

            // 4. 计算可用库存 = 总库存 - 锁定库存
            int totalStock = stock.getStock();
            int lockStock = stock.getLockStock();
            int usableStock = totalStock - lockStock;

            // 5. 可用库存不足判断
            if (usableStock < dto.getOutboundQty()) {
                return Result.fail("库存不足，当前可用库存：" + usableStock);
            }
            // ====================================================================

            // 6. 构建出库单实体
            InventoryOut inventoryOut = new InventoryOut();

            // 7. 业务字段赋值
            inventoryOut.setGoodsId(goodsId);
            inventoryOut.setGoodsName(dto.getGoodsName());
            inventoryOut.setOutboundQty(dto.getOutboundQty());
            inventoryOut.setRemark(dto.getRemark());

            // 8. 系统字段赋值
            inventoryOut.setCreateTime(LocalDateTime.now());

            // 9. 生成排序号（当前最大 sort + 1）
            Integer maxSort = stockOutMapper.selectMaxSort();
            int sort;
            if (maxSort == null) {
                sort = 1;
            } else {
                sort = maxSort + 1;
            }
            inventoryOut.setSort(sort);

            // 10. 生成唯一出库单号
            String outboundNo = OrderNoGenerator.generate(OrderPrefix.OUTBOUND);
            inventoryOut.setOutboundNo(outboundNo);

            // 11. 获取登录用户（操作人）
            LoginUserVO user = LoginUserContext.getUser();
            if (user == null) {
                return Result.fail("未获取到登录用户信息，无法新增出库单");
            }
            inventoryOut.setOperator(user.getUsername());

            // 12. 插入出库单数据
            stockOutMapper.insert(inventoryOut);

            // ===================== 核心：调用库存服务扣减可用库存 =====================
            // 扣减库存 → 自动记录流水 → 自动刷新库存状态
            stockService.decreaseStock(goodsId, dto.getOutboundQty());

            // 13. 返回成功信息
            return Result.success("新增出库单成功，出库单号：" + outboundNo);

        } catch (Exception e) {
            // 14. 异常捕获，事务自动回滚
            return Result.fail("新增出库单失败：" + e.getMessage());
        }
    }

}