package com.inventory.modules.invertory.stockin.service.impl;

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
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.entity.InventoryIn;
import com.inventory.modules.invertory.stockin.service.InventoryInService;
import com.inventory.modules.invertory.stockin.mapper.InventoryInMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.LoginContext;
import java.time.LocalDateTime;

/**
 * 入库单管理 Service 实现类
 *
 * 功能说明：
 * 1. 入库单分页查询、详情查询
 * 2. 新增入库单（自动生成单号 + 自动增加库存）
 * 3. 事务保证：入库单创建 + 库存增加 要么都成功，要么都失败
 *
 * @author 95349
 */
@Service
@RequiredArgsConstructor
public class InventoryInServiceImpl extends ServiceImpl<InventoryInMapper, InventoryIn>
        implements InventoryInService {

    /**
     * 入库单 Mapper
     */
    private final InventoryInMapper stockInMapper;

    /**
     * 库存核心服务（增加库存、写流水、刷新状态）
     */
    private final StockService stockService;

    /**
     * 库存表 Mapper
     */
    private final InventoryStockMapper inventoryStockMapper;

    /**
     * 商品基础 Mapper
     */
    private final GoodsProductMapper goodsProductMapper;

    // ======================== 1. 入库单分页查询 ========================
    /**
     * 入库单分页多条件查询
     * 支持条件：入库单号、商品名称、创建时间范围
     * 排序：创建时间倒序（最新在前）
     */
    @Override
    public Result<?> pageStockIn(String receiptNo, String goodsName, String startTime, String endTime,
                                 Long pageNum, Long pageSize) {
        try {
            // 1. 初始化分页对象
            Page<InventoryIn> page = new Page<>(pageNum, pageSize);

            // 2. 构建查询条件
            LambdaQueryWrapper<InventoryIn> wrapper = Wrappers.lambdaQuery();

            // 3. 入库单号精确匹配
            if (StrUtil.isNotBlank(receiptNo)) {
                wrapper.eq(InventoryIn::getReceiptNo, receiptNo);
            }

            // 4. 商品名称模糊匹配
            if (StrUtil.isNotBlank(goodsName)) {
                wrapper.like(InventoryIn::getGoodsName, goodsName);
            }

            // 5. 开始时间：创建时间 >= startTime
            if (StrUtil.isNotBlank(startTime)) {
                wrapper.ge(InventoryIn::getCreateTime, startTime);
            }

            // 6. 结束时间：创建时间 <= endTime
            if (StrUtil.isNotBlank(endTime)) {
                wrapper.le(InventoryIn::getCreateTime, endTime);
            }

            // 7. 排序规则：按创建时间倒序
            wrapper.orderByDesc(InventoryIn::getCreateTime);

            // 8. 执行分页查询
            Page<InventoryIn> resultPage = stockInMapper.selectPage(page, wrapper);

            // 9. 返回查询成功结果
            return Result.success(resultPage);

        } catch (Exception e) {
            // 10. 异常捕获，返回友好提示
            return Result.fail("入库单分页查询失败：" + e.getMessage());
        }
    }

    // ======================== 2. 查询入库单详情 ========================
    /**
     * 根据主键ID查询入库单详情
     */
    @Override
    public Result<?> getStockInDetail(Long id) {
        try {
            // 1. 根据ID查询单条数据
            InventoryIn inventoryIn = stockInMapper.selectById(id);

            // 2. 判断数据是否存在
            if (inventoryIn == null) {
                return Result.fail("入库单不存在");
            }

            // 3. 返回数据
            return Result.success(inventoryIn);

        } catch (Exception e) {
            // 4. 异常处理
            return Result.fail("查询入库单详情失败：" + e.getMessage());
        }
    }

    // ======================== 3. 新增入库单 + 自动增加库存 ========================
    /**
     * 新增入库单
     * 逻辑：
     * 1. 参数校验与转换
     * 2. 生成唯一入库单号
     * 3. 填充操作人、创建时间
     * 4. 保存入库单
     * 5. 调用库存服务增加可用库存（事务保证）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addStockIn(StockInAddDTO dto) {
        try {
            // 1. 构建入库单实体对象
            InventoryIn inventoryIn = new InventoryIn();

            // 2. 商品ID格式转换与校验
            Long goodsId;
            try {
                goodsId = Long.valueOf(dto.getGoodsId());
            } catch (NumberFormatException e) {
                return Result.fail("商品ID格式错误，必须为数字");
            }

            // 3. 业务字段赋值
            inventoryIn.setGoodsId(goodsId);
            inventoryIn.setGoodsName(dto.getGoodsName());
            inventoryIn.setReceiptQty(dto.getReceiptQty());
            inventoryIn.setRemark(dto.getRemark());

            // 4. 系统字段赋值：创建时间
            inventoryIn.setCreateTime(LocalDateTime.now());

            // 5. 生成唯一入库单号（规则：前缀+时间+序列）
            String receiptNo = OrderNoGenerator.generate(OrderPrefix.INBOUND);
            inventoryIn.setReceiptNo(receiptNo);

            // 6. 获取当前登录用户（操作人）
            LoginUserVO user = LoginUserContext.getUser();
            if (user == null) {
                return Result.fail("未获取到登录用户信息，无法新增入库");
            }
            inventoryIn.setOperator(user.getUsername());

            // 7. 插入入库单数据
            stockInMapper.insert(inventoryIn);

            // ===================== 核心：调用库存服务增加可用库存 =====================
            // 入库操作 → 库存数量增加 → 自动记录流水 → 自动刷新库存状态
            stockService.increaseStockFlow(goodsId, dto.getReceiptQty(),receiptNo);
            // 8. 返回成功信息（带入库单号）
            return Result.success("新增入库单成功，入库单号：" + receiptNo);

        } catch (Exception e) {
            // 9. 异常捕获，事务自动回滚
            return Result.fail("新增入库单失败：" + e.getMessage());
        }
    }

}