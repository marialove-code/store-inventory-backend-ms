package com.inventory.order.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.inventory.common.client.dto.LockStockFlowContext;
import com.inventory.common.client.dto.ResetStockRequest;
import com.inventory.common.client.dto.WriteFlowRequest;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import com.inventory.order.client.dto.StockCommandRequest;
import com.inventory.order.config.SentinelResourceNames;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存服务远程客户端门面。
 * <p>
 * 对业务层仍暴露原来的方法签名；底层由 {@link InventoryStockFeignClient}
 * 经 Nacos 按服务名 {@code inventory-service} 调用，不再使用写死的 base-url。
 * </p>
 * <p>
 * 解析统一 {@link Result}：code != 200 或 Feign/网络失败时抛 {@link BusinessException}，不吞异常。
 * {@code lock}/{@code unlock} 挂 Sentinel 熔断：库存服务异常比例过高时快速失败。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryStockClient {

    private final InventoryStockFeignClient feignClient;

    /**
     * 锁定库存（下单预占，带业务单号）。对应库存 POST /inventory/internal/lock
     */
    @SentinelResource(
            value = SentinelResourceNames.INVENTORY_LOCK,
            blockHandler = "lockBlockHandler"
    )
    public void lock(Long goodsId, Integer qty, String orderNo) {
        postCommand(() -> feignClient.lock(StockCommandRequest.of(goodsId, qty, orderNo)), "锁定库存");
    }

    /**
     * 锁库存被熔断/限流时：转为业务异常，由下单流程统一返回。
     */
    public void lockBlockHandler(Long goodsId, Integer qty, String orderNo, BlockException ex) {
        throw new BusinessException(ResultCode.FAIL.getCode(),
                "锁定库存触发 Sentinel 防护（资源 inventoryLock）：" + ex.getClass().getSimpleName());
    }

    /**
     * 非原子锁定库存（V1/V2 压测基线）。不传 bizNo。
     */
    public void lockNonAtomic(Long goodsId, Integer qty) {
        postCommand(() -> feignClient.lock(StockCommandRequest.of(goodsId, qty)), "非原子锁定库存");
    }

    /**
     * V3：仅更新 lock_stock，不写流水。
     */
    public LockStockFlowContext lockUpdateOnly(Long goodsId, Integer qty) {
        try {
            Result<LockStockFlowContext> result =
                    feignClient.lockUpdateOnly(StockCommandRequest.of(goodsId, qty));
            assertSuccess(result, "仅更新锁定库存");
            return result.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.error("【库存远程】仅更新锁定库存 Feign 失败 goodsId={}, qty={}", goodsId, qty, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（仅更新锁定库存）：" + ex.getMessage());
        }
    }

    /**
     * 写入库存流水（V3 异步补写）。
     */
    public void writeFlow(
            Long goodsId,
            String goodsName,
            Integer beforeStock,
            Integer changeStock,
            Integer afterStock,
            Integer operateType,
            String bizNo,
            String remark) {
        WriteFlowRequest req = new WriteFlowRequest();
        req.setGoodsId(goodsId);
        req.setGoodsName(goodsName);
        req.setBeforeStock(beforeStock);
        req.setChangeStock(changeStock);
        req.setAfterStock(afterStock);
        req.setOperateType(operateType);
        req.setBizNo(bizNo);
        req.setRemark(remark);

        postCommand(() -> feignClient.writeFlow(req), "写入库存流水");
    }

    /**
     * 压测辅助：重置商品 stock / lockStock。
     */
    public void resetStock(Long goodsId, Integer stock, Integer lockStock) {
        ResetStockRequest req = new ResetStockRequest();
        req.setGoodsId(goodsId);
        req.setStock(stock);
        req.setLockStock(lockStock);
        postCommand(() -> feignClient.resetStock(req), "重置库存");
    }

    /**
     * 释放锁定库存（取消订单）。
     */
    @SentinelResource(
            value = SentinelResourceNames.INVENTORY_UNLOCK,
            blockHandler = "unlockBlockHandler"
    )
    public void unlock(Long goodsId, Integer qty) {
        postCommand(() -> feignClient.unlock(StockCommandRequest.of(goodsId, qty)), "解锁库存");
    }

    public void unlockBlockHandler(Long goodsId, Integer qty, BlockException ex) {
        throw new BusinessException(ResultCode.FAIL.getCode(),
                "解锁库存触发 Sentinel 防护（资源 inventoryUnlock）：" + ex.getClass().getSimpleName());
    }

    /**
     * 发货扣减：账面库存与 lock_stock 同步减少，并记流水。
     */
    public void decreaseFlow(Long goodsId, Integer qty, String logisticsNo) {
        StockCommandRequest req = StockCommandRequest.of(goodsId, qty, logisticsNo);
        req.setReceiptNo(logisticsNo);
        postCommand(() -> feignClient.decreaseFlow(req), "发货扣减库存");
    }

    /**
     * 增加库存（退货回库）。
     */
    public void increase(Long goodsId, Integer qty) {
        postCommand(() -> feignClient.increase(StockCommandRequest.of(goodsId, qty)), "增加库存");
    }

    /**
     * 查询可用库存快照：stock / lockStock / usableStock / goodsName。
     */
    public Map<String, Object> getUsable(Long goodsId) {
        try {
            Result<Map<String, Object>> result = feignClient.usable(goodsId);
            assertSuccess(result, "查询可用库存");
            return result.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.error("【库存远程】查询可用库存 Feign 失败 goodsId={}", goodsId, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（查询可用库存）：" + ex.getMessage());
        }
    }

    /**
     * 从快照中解析可用数量。
     * <p>
     * 下单入口调的是本方法（不是 {@link #getUsable}）。
     * Sentinel 必须挂在这里：同类内部 {@code this.getUsable()} 不会走 AOP，挂在 getUsable 上等于没挂。
     * </p>
     */
    @SentinelResource(
            value = SentinelResourceNames.INVENTORY_USABLE,
            blockHandler = "usableStockBlockHandler"
    )
    public int getUsableStock(Long goodsId) {
        Map<String, Object> snapshot = getUsable(goodsId);
        if (snapshot == null || snapshot.get("usableStock") == null) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "库存服务未返回 usableStock");
        }
        Object raw = snapshot.get("usableStock");
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        throw new BusinessException(ResultCode.FAIL.getCode(), "usableStock 类型异常：" + raw);
    }

    public int usableStockBlockHandler(Long goodsId, BlockException ex) {
        throw new BusinessException(ResultCode.FAIL.getCode(),
                "查询可用库存触发 Sentinel 防护（资源 inventoryUsable）：" + ex.getClass().getSimpleName());
    }

    /**
     * 无返回体的 POST 命令：调 Feign → 校验 Result。
     */
    private void postCommand(FeignVoidCall call, String actionLabel) {
        try {
            assertSuccess(call.execute(), actionLabel);
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.error("【库存远程】{} Feign 失败", actionLabel, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（" + actionLabel + "）：" + ex.getMessage());
        }
    }

    /**
     * 校验 Result：null 或 code != 200 一律抛 BusinessException（不吞）。
     */
    private void assertSuccess(Result<?> result, String actionLabel) {
        if (result == null) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "库存服务无响应（" + actionLabel + "）");
        }
        if (result.getCode() != ResultCode.SUCCESS.getCode()) {
            String msg = result.getMessage() != null ? result.getMessage() : actionLabel + "失败";
            throw new BusinessException(result.getCode(), msg);
        }
    }

    @FunctionalInterface
    private interface FeignVoidCall {
        Result<Void> execute();
    }
}
