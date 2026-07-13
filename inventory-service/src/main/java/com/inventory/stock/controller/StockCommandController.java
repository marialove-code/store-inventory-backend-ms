package com.inventory.stock.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.stock.dto.StockCommandRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存命令式 HTTP API（供订单服务后续跨进程调用）。
 * <p>
 * 路径前缀 {@code /inventory/internal/**}，表示「服务间内部接口」，
 * 本阶段无鉴权；后续可加网关鉴权或内网隔离。
 * 每个方法注释写明对应单体中的订单场景。
 * </p>
 */
@RestController
@RequestMapping("/inventory/internal")
@RequiredArgsConstructor
public class StockCommandController {

    private final StockService stockService;
    private final InventoryStockMapper inventoryStockMapper;

    /**
     * 锁定库存（订单预占）。
     * <p>
     * 对应单体：{@code OrderInfoServiceImpl} 创建订单时调用 {@code stockService.lockStock}。
     * 若传入 bizNo（订单号），使用 V4 原子锁定 {@code lockStockAtomically}，防止超卖；
     * 否则走普通 {@code lockStock}。
     * </p>
     */
    @PostMapping("/lock")
    public Result<Void> lock(@Valid @RequestBody StockCommandRequest req) {
        String orderNo = resolveBizNo(req);
        if (StringUtils.hasText(orderNo)) {
            stockService.lockStockAtomically(req.getGoodsId(), req.getQty(), orderNo);
        } else {
            stockService.lockStock(req.getGoodsId(), req.getQty());
        }
        return Result.success();
    }

    /**
     * 释放锁定库存（取消订单）。
     * <p>
     * 对应单体：{@code OrderInfoServiceImpl} 取消订单时调用 {@code stockService.unlockStock}。
     * </p>
     */
    @PostMapping("/unlock")
    public Result<Void> unlock(@Valid @RequestBody StockCommandRequest req) {
        stockService.unlockStock(req.getGoodsId(), req.getQty());
        return Result.success();
    }

    /**
     * 发货扣减：减少账面库存，并同步减少 lock_stock。
     * <p>
     * 对应单体：{@code OrderDeliveryServiceImpl} 发货时调用
     * {@code stockService.decreaseStockFlow(goodsId, buyQty, logisticsNo)}。
     * receiptNo / bizNo 写入流水业务单号（通常为物流单号）。
     * </p>
     */
    @PostMapping("/decrease-flow")
    public Result<Void> decreaseFlow(@Valid @RequestBody StockCommandRequest req) {
        stockService.decreaseStockFlow(req.getGoodsId(), req.getQty(), resolveBizNo(req));
        return Result.success();
    }

    /**
     * 增加库存（退货回库 / 补货）。
     * <p>
     * 对应单体：
     * <ul>
     *   <li>退货：{@code OrderRefundServiceImpl} → {@code increaseStock}</li>
     *   <li>入库带单号：{@code InventoryInServiceImpl} → {@code increaseStockFlow}</li>
     * </ul>
     * 有单号则写流水 bizNo，无单号则走普通增加。
     * </p>
     */
    @PostMapping("/increase")
    public Result<Void> increase(@Valid @RequestBody StockCommandRequest req) {
        String bizNo = resolveBizNo(req);
        if (StringUtils.hasText(bizNo)) {
            stockService.increaseStockFlow(req.getGoodsId(), req.getQty(), bizNo);
        } else {
            stockService.increaseStock(req.getGoodsId(), req.getQty());
        }
        return Result.success();
    }

    /**
     * 查询可用库存：{@code stock - lock_stock}。
     * <p>
     * 对应单体订单创建前的库存校验思路；供订单服务下单前探活可用量。
     * </p>
     *
     * @param goodsId 商品 ID
     */
    @GetMapping("/usable")
    public Result<Map<String, Object>> usable(@RequestParam Long goodsId) {
        LambdaQueryWrapper<InventoryStock> qw = new LambdaQueryWrapper<>();
        qw.eq(InventoryStock::getGoodsId, goodsId);
        InventoryStock stock = inventoryStockMapper.selectOne(qw);
        if (stock == null) {
            return Result.fail("商品库存不存在");
        }
        int total = stock.getStock() == null ? 0 : stock.getStock();
        int locked = stock.getLockStock() == null ? 0 : stock.getLockStock();
        int usable = total - locked;

        Map<String, Object> data = new HashMap<>(8);
        data.put("goodsId", goodsId);
        data.put("stock", total);
        data.put("lockStock", locked);
        data.put("usableStock", usable);
        return Result.success(data);
    }

    /**
     * 解析业务单号：优先 bizNo，其次 receiptNo。
     */
    private String resolveBizNo(StockCommandRequest req) {
        if (StringUtils.hasText(req.getBizNo())) {
            return req.getBizNo();
        }
        return req.getReceiptNo();
    }
}
