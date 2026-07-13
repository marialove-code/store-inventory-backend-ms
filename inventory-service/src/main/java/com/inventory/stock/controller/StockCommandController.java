package com.inventory.stock.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.inventory.common.client.dto.LockStockFlowContext;
import com.inventory.common.client.dto.ResetStockRequest;
import com.inventory.common.client.dto.StockInitRequest;
import com.inventory.common.client.dto.WriteFlowRequest;
import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.entity.InventoryStock;
import com.inventory.modules.invertory.stock.mapper.InventoryStockMapper;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.stock.dto.StockCommandRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     * 平台新增商品时初始化库存记录。
     * <p>
     * 对应单体：{@code GoodsProductServiceImpl#addProduct} 本地 insert InventoryStock。
     * 拆分后由 platform 调本接口写入；同一 {@code goodsId} 已存在记录则直接成功（幂等），避免重复插入。
     * </p>
     * <p>
     * 默认值：stock=0、stockWarn=10、lockStock=0、stockStatus=1（正常），时间取 now。
     * </p>
     */
    @PostMapping("/init-stock")
    public Result<Void> initStock(@RequestBody StockInitRequest req) {
        if (req == null || req.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }
        // 幂等：已有该商品库存记录则直接成功
        LambdaQueryWrapper<InventoryStock> qw = new LambdaQueryWrapper<>();
        qw.eq(InventoryStock::getGoodsId, req.getGoodsId());
        InventoryStock existing = inventoryStockMapper.selectOne(qw);
        if (existing != null) {
            return Result.success();
        }

        int stock = req.getStock() != null ? req.getStock() : 0;
        int stockWarn = req.getStockWarn() != null ? req.getStockWarn() : 10;
        int lockStock = req.getLockStock() != null ? req.getLockStock() : 0;

        InventoryStock entity = new InventoryStock();
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setGoodsId(req.getGoodsId());
        entity.setGoodsName(req.getGoodsName());
        entity.setCategoryName(req.getCategoryName());
        entity.setStock(stock);
        entity.setLockStock(lockStock);
        entity.setStockWarn(stockWarn);
        // 新建默认正常；后续入库/预警任务会再刷新状态
        entity.setStockStatus(1);
        entity.setSort(0);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        inventoryStockMapper.insert(entity);
        return Result.success();
    }

    /**
     * 查询可用库存：{@code stock - lock_stock}。
     * <p>
     * 对应单体订单创建前的库存校验思路；供订单服务下单前探活可用量。
     * 平台删除商品前也可据此判断账面 stock 是否 &gt; 0。
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
        data.put("goodsName", stock.getGoodsName());
        data.put("stock", total);
        data.put("lockStock", locked);
        data.put("usableStock", usable);
        return Result.success(data);
    }

    /**
     * V3 专用：仅更新 lock_stock，不写流水。
     * <p>
     * 对应 {@code StockService#lockStockUpdateOnly}。
     * 流水由订单侧异步调用 {@code /write-flow} 补写，以缩短同步路径耗时。
     * </p>
     *
     * @param req goodsId + qty
     * @return 异步写流水所需的 before/after 上下文
     */
    @PostMapping("/lock-update-only")
    public Result<LockStockFlowContext> lockUpdateOnly(@Valid @RequestBody StockCommandRequest req) {
        LockStockFlowContext ctx = stockService.lockStockUpdateOnly(req.getGoodsId(), req.getQty());
        return Result.success(ctx);
    }

    /**
     * 写入库存流水（V3 异步补写等场景）。
     * <p>
     * 对应 {@code StockService#writeFlow}。
     * </p>
     */
    @PostMapping("/write-flow")
    public Result<Void> writeFlow(@RequestBody WriteFlowRequest req) {
        if (req == null || req.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }
        stockService.writeFlow(
                req.getGoodsId(),
                req.getGoodsName(),
                req.getBeforeStock(),
                req.getChangeStock(),
                req.getAfterStock(),
                req.getOperateType(),
                req.getBizNo(),
                req.getRemark()
        );
        return Result.success();
    }

    /**
     * 压测辅助：重置指定商品的 stock / lockStock。
     * <p>
     * <b>仅压测辅助，后续可加 Profile（如 {@code @Profile("dev")}）限制。</b>
     * 幂等更新：按 goodsId 覆盖写入，重复调用结果一致。
     * </p>
     *
     * @param req goodsId、stock、lockStock（也可通过 query 传参，见下方重载）
     */
    @PostMapping("/dev/reset-stock")
    public Result<Void> resetStock(
            @RequestBody(required = false) ResetStockRequest req,
            @RequestParam(required = false) Long goodsId,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) Integer lockStock) {

        Long targetGoodsId = goodsId;
        Integer targetStock = stock;
        Integer targetLockStock = lockStock;
        if (req != null) {
            if (req.getGoodsId() != null) {
                targetGoodsId = req.getGoodsId();
            }
            if (req.getStock() != null) {
                targetStock = req.getStock();
            }
            if (req.getLockStock() != null) {
                targetLockStock = req.getLockStock();
            }
        }

        if (targetGoodsId == null) {
            return Result.fail("goodsId 不能为空");
        }
        if (targetStock == null || targetStock < 0) {
            return Result.fail("stock 无效");
        }
        if (targetLockStock == null || targetLockStock < 0) {
            return Result.fail("lockStock 无效");
        }

        LambdaUpdateWrapper<InventoryStock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(InventoryStock::getGoodsId, targetGoodsId)
                .set(InventoryStock::getStock, targetStock)
                .set(InventoryStock::getLockStock, targetLockStock)
                .set(InventoryStock::getUpdateTime, LocalDateTime.now());

        int rows = inventoryStockMapper.update(null, updateWrapper);
        if (rows == 0) {
            return Result.fail("未找到该商品的库存记录，goodsId=" + targetGoodsId);
        }
        return Result.success();
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
