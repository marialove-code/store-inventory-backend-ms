package com.inventory.platform.client;

import com.inventory.common.client.dto.StockInitRequest;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存服务远程客户端门面（平台侧）。
 * <p>
 * 对业务层仍暴露原方法签名；底层由 {@link InventoryStockFeignClient}
 * 经 Nacos 按服务名 {@code inventory-service} 调用。
 * </p>
 * <p>
 * 主要场景：新增商品 {@link #initStock}；删除前 {@link #getStockSnapshot} 校验账面库存。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryStockClient {

    private final InventoryStockFeignClient feignClient;

    /**
     * 平台新增商品后初始化库存。
     * 对应库存 POST /inventory/internal/init-stock（幂等：已有记录则成功返回）。
     */
    public void initStock(StockInitRequest request) {
        try {
            assertSuccess(feignClient.initStock(request), "初始化库存");
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.error("【库存远程】初始化库存 Feign 失败 body={}", request, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（初始化库存）：" + ex.getMessage());
        }
    }

    /**
     * 查询可用库存快照：stock / lockStock / usableStock。
     * 若库存记录不存在，返回 {@code null}；其它业务失败仍抛 {@link BusinessException}。
     */
    public Map<String, Object> getUsable(Long goodsId) {
        return getStockSnapshot(goodsId);
    }

    /**
     * 从快照中解析可用数量；记录不存在时抛业务异常。
     */
    public int getUsableStock(Long goodsId) {
        Map<String, Object> snapshot = getStockSnapshot(goodsId);
        if (snapshot == null || snapshot.get("usableStock") == null) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "库存服务未返回 usableStock（或商品库存不存在）");
        }
        Object raw = snapshot.get("usableStock");
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        throw new BusinessException(ResultCode.FAIL.getCode(), "usableStock 类型异常：" + raw);
    }

    /**
     * 查询库存快照。
     * <p>
     * 删除商品前：返回 null（记录不存在）可视为可删；{@code stock &gt; 0} 则不允许删除。
     * </p>
     */
    public Map<String, Object> getStockSnapshot(Long goodsId) {
        try {
            Result<Map<String, Object>> result = feignClient.usable(goodsId);
            if (result == null) {
                throw new BusinessException(ResultCode.FAIL.getCode(), "库存服务无响应（查询库存快照）");
            }
            // 记录不存在：库存侧返回业务失败文案，平台删除场景视为可删
            if (result.getCode() != ResultCode.SUCCESS.getCode()) {
                String msg = result.getMessage() != null ? result.getMessage() : "";
                if (msg.contains("不存在")) {
                    return null;
                }
                throw new BusinessException(result.getCode(), msg.isEmpty() ? "查询库存快照失败" : msg);
            }
            return result.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.error("【库存远程】查询库存快照 Feign 失败 goodsId={}", goodsId, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（查询库存快照）：" + ex.getMessage());
        }
    }

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
}
