package com.inventory.order.client;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import com.inventory.order.client.dto.StockCommandRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 库存服务远程客户端（RestTemplate）。
 * <p>
 * 封装对 {@code inventory-service} 内部命令 API 的调用：
 * {@code /inventory/internal/lock|unlock|decrease-flow|increase|usable}。
 * </p>
 * <p>
 * <b>设计约束：</b>
 * <ul>
 *   <li>订单服务禁止依赖 StockService / InventoryStockMapper</li>
 *   <li>本阶段不使用 Feign / Nacos，base-url 可配置</li>
 *   <li>解析统一 {@link Result}：code != 200 或 HTTP/网络失败时抛 {@link BusinessException}，不吞异常</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class InventoryStockClient {

    private final RestTemplate restTemplate;

    /**
     * 库存服务根地址，如 http://localhost:8082/api
     * （已含 context-path，后面再拼 /inventory/internal/**）
     */
    private final String baseUrl;

    public InventoryStockClient(
            RestTemplate restTemplate,
            @Value("${inventory.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        // 去掉末尾斜杠，避免拼接出双斜杠
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 锁定库存（下单预占）。对应库存 POST /inventory/internal/lock
     *
     * @param goodsId 商品 ID
     * @param qty     锁定数量
     * @param orderNo 订单号，作为 bizNo 传入，库存侧可走原子锁
     */
    public void lock(Long goodsId, Integer qty, String orderNo) {
        postCommand("/inventory/internal/lock", StockCommandRequest.of(goodsId, qty, orderNo), "锁定库存");
    }

    /**
     * 释放锁定库存（取消订单）。对应库存 POST /inventory/internal/unlock
     */
    public void unlock(Long goodsId, Integer qty) {
        postCommand("/inventory/internal/unlock", StockCommandRequest.of(goodsId, qty), "解锁库存");
    }

    /**
     * 发货扣减：账面库存与 lock_stock 同步减少，并记流水。
     * 对应库存 POST /inventory/internal/decrease-flow
     *
     * @param logisticsNo 物流单号，写入流水业务单号
     */
    public void decreaseFlow(Long goodsId, Integer qty, String logisticsNo) {
        StockCommandRequest req = StockCommandRequest.of(goodsId, qty, logisticsNo);
        req.setReceiptNo(logisticsNo);
        postCommand("/inventory/internal/decrease-flow", req, "发货扣减库存");
    }

    /**
     * 增加库存（退货回库）。对应库存 POST /inventory/internal/increase
     */
    public void increase(Long goodsId, Integer qty) {
        postCommand("/inventory/internal/increase", StockCommandRequest.of(goodsId, qty), "增加库存");
    }

    /**
     * 查询可用库存快照：stock / lockStock / usableStock。
     * 对应库存 GET /inventory/internal/usable?goodsId=
     *
     * @return 库存快照 Map（至少含 usableStock）
     */
    public Map<String, Object> getUsable(Long goodsId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/inventory/internal/usable")
                .queryParam("goodsId", goodsId)
                .toUriString();
        try {
            ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<Map<String, Object>>>() {
                    }
            );
            Result<Map<String, Object>> result = response.getBody();
            assertSuccess(result, "查询可用库存");
            return result.getData();
        } catch (BusinessException ex) {
            // 业务异常原样抛出，不要包装成「库存调用失败」掩盖真实原因
            throw ex;
        } catch (RestClientException ex) {
            log.error("【库存远程】查询可用库存 HTTP 失败 goodsId={}", goodsId, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（查询可用库存）：" + ex.getMessage());
        }
    }

    /**
     * 从快照中解析可用数量；缺字段或类型异常时抛业务异常。
     */
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

    /**
     * POST 命令类接口的通用封装：发请求 → 解析 Result → 失败抛异常。
     */
    private void postCommand(String path, StockCommandRequest body, String actionLabel) {
        String url = baseUrl + path;
        try {
            ResponseEntity<Result<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    new ParameterizedTypeReference<Result<Void>>() {
                    }
            );
            assertSuccess(response.getBody(), actionLabel);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("【库存远程】{} HTTP 失败 url={}, body={}", actionLabel, url, body, ex);
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
}
