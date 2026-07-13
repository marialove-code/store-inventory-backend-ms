package com.inventory.modules.shop.records.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.inventory.common.response.Result;
import com.inventory.modules.shop.dashboard.vo.DashboardShopVO;
import com.inventory.modules.shop.dashboard.vo.ShopHotProductVO;
import com.inventory.modules.shop.dashboard.vo.ShopStockWarnVO;
import com.inventory.modules.shop.product.entity.ShopProduct;
import com.inventory.modules.shop.product.service.ShopProductService;
import com.inventory.modules.shop.records.dto.ShopSaleCreateDto;
import com.inventory.modules.shop.records.entity.ShopSaleRecord;
import com.inventory.modules.shop.records.entity.ShopSaleRecordListParam;
import com.inventory.modules.shop.records.mapper.ShopSaleRecordMapper;
import com.inventory.modules.shop.records.service.ShopSaleRecordService;
import com.inventory.modules.shop.records.vo.ShopSaleRecordVo;
import com.inventory.modules.shop.records.vo.ShopSaleStatsVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopSaleRecordServiceImpl extends ServiceImpl<ShopSaleRecordMapper, ShopSaleRecord>
        implements ShopSaleRecordService {


    private final ShopSaleRecordMapper shopSaleRecordMapper;
    private final ShopProductService shopProductService;

    /**
     * 销售流水分页
     */
    @Override
    public Result<?> getSaleRecordPage(ShopSaleRecordListParam param) {
        Page<ShopSaleRecord> page = new Page<>(param.getPageNum(), param.getPageSize());

        LambdaQueryWrapper<ShopSaleRecord> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(ShopSaleRecord::getIsDeleted, 0);

        // 商品名称模糊搜索
        if (StrUtil.isNotBlank(param.getKeyword())) {
            wrapper.like(ShopSaleRecord::getProductName, param.getKeyword());
        }

        applySaleDateFilter(wrapper, param);

        wrapper.orderByDesc(ShopSaleRecord::getSaleTime);

        Page<ShopSaleRecord> salePage = this.page(page, wrapper);

        Page<ShopSaleRecordVo> voPage = new Page<>(
                salePage.getCurrent(),
                salePage.getSize(),
                salePage.getTotal()
        );

        List<ShopSaleRecordVo> voList = salePage.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, ShopSaleRecordVo.class))
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 开单（扣库存 + 记流水 + 算利润）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createSaleOrder(ShopSaleCreateDto dto) {
        // 查询商品
        ShopProduct product = shopProductService.getById(dto.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail("商品不存在");
        }

        // 库存判断
        if (product.getStock() < dto.getQuantity()) {
            return Result.fail("库存不足");
        }

        // 成交价
        BigDecimal salePrice = dto.getSalePrice() != null ? dto.getSalePrice() : product.getSalePrice();

        // 计算金额 & 利润
        BigDecimal qty = new BigDecimal(dto.getQuantity());
        BigDecimal totalAmount = salePrice.multiply(qty);
        BigDecimal profit = salePrice.subtract(product.getCostPrice()).multiply(qty);

        // 扣库存
        product.setStock(product.getStock() - dto.getQuantity());
        shopProductService.updateById(product);

        // 写入流水
        ShopSaleRecord record = new ShopSaleRecord();
        record.setProductId(product.getId());
        record.setProductName(product.getProductName());
        record.setSalePrice(salePrice);
        record.setQuantity(dto.getQuantity());
        record.setTotalAmount(totalAmount);
        record.setProfit(profit);
        record.setSaleTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setIsDeleted(0);

        boolean save = this.save(record);
        return Result.success(save);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteSaleRecord(Long id) {
        ShopSaleRecord record = this.getById(id);
        if (record == null || record.getIsDeleted() == 1) {
            return Result.fail("销售记录不存在");
        }

        LambdaUpdateWrapper<ShopSaleRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShopSaleRecord::getId, id);
        wrapper.set(ShopSaleRecord::getIsDeleted, 1);
        this.update(wrapper);
        return Result.success("删除成功");
    }

    /**
     * 按售卖年月日筛选（可只选年，或年+月，或完整日期）
     */
    private void applySaleDateFilter(LambdaQueryWrapper<ShopSaleRecord> wrapper, ShopSaleRecordListParam param) {
        Integer year = param.getSaleYear();
        Integer month = param.getSaleMonth();
        Integer day = param.getSaleDay();

        if (year == null) {
            return;
        }

        if (month != null && day != null) {
            LocalDate date = LocalDate.of(year, month, day);
            wrapper.ge(ShopSaleRecord::getSaleTime, date.atStartOfDay());
            wrapper.lt(ShopSaleRecord::getSaleTime, date.plusDays(1).atStartOfDay());
            return;
        }

        if (month != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1);
            wrapper.ge(ShopSaleRecord::getSaleTime, start.atStartOfDay());
            wrapper.lt(ShopSaleRecord::getSaleTime, end.atStartOfDay());
            return;
        }

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = start.plusYears(1);
        wrapper.ge(ShopSaleRecord::getSaleTime, start.atStartOfDay());
        wrapper.lt(ShopSaleRecord::getSaleTime, end.atStartOfDay());
    }

    /**
     * 销售统计
     */
    @Override
    public Result<?> getSaleStatistics() {
        ShopSaleStatsVo vo = shopSaleRecordMapper.selectSaleTotalStats();
        // 防止null空指针，默认0
        if(vo.getTodayAmount() == null){
            vo.setTodayAmount(BigDecimal.ZERO);
        }
        if(vo.getMonthAmount() == null){
            vo.setMonthAmount(BigDecimal.ZERO);
        }
        return Result.success(vo);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<?> getShopDashboardInfo(String year) {
        // 不传年份默认取系统当前年
        Integer targetYear = year == null ? LocalDate.now().getYear() : Integer.parseInt(year);
        DashboardShopVO vo = baseMapper.selectDashboardSaleData(targetYear);
        //热销TOP5（同年份筛选）
        List<ShopHotProductVO> top5 = baseMapper.selectHotTop5Product(targetYear);
        vo.setHotTop5(top5);
        //库存预警不受年份影响，直接查全量
        List<ShopStockWarnVO> warnList = shopProductService.getWarnStockList();
        vo.setStockWarnList(warnList);
        return Result.success(vo);
    }
}