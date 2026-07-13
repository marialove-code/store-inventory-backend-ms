package com.inventory.platform.client;

import com.inventory.common.client.dto.StockInitRequest;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
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
 * 库存服务远程客户端（平台侧，RestTemplate）。
 * <p>
 * 封装对 {@code inventory-service} 内部 API 的调用，风格对齐 order-service 的
 * {@code InventoryStockClient}：解析统一 {@link Result}，失败抛 {@link BusinessException}，不吞异常。
 * </p>
 * <p>
 * 主要场景：
 * <ul>
 *   <li>新增商品后 {@link #initStock} 初始化库存记录</li>
 *   <li>删除商品前 {@link #getStockSnapshot} / {@link #getUsable} 校验账面库存是否 &gt; 0</li>
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
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 平台新增商品后初始化库存。
     * 对应库存 POST /inventory/internal/init-stock（幂等：已有记录则成功返回）。
     *
     * @param request 含 goodsId / goodsName / categoryName / stockWarn 等
     */
    public void initStock(StockInitRequest request) {
        String url = baseUrl + "/inventory/internal/init-stock";
        try {
            ResponseEntity<Result<Void>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<Result<Void>>() {
                    }
            );
            assertSuccess(response.getBody(), "初始化库存");
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("【库存远程】初始化库存 HTTP 失败 url={}, body={}", url, request, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（初始化库存）：" + ex.getMessage());
        }
    }

    /**
     * 查询可用库存快照：stock / lockStock / usableStock。
     * 对应库存 GET /inventory/internal/usable?goodsId=
     * <p>
     * 若库存记录不存在，返回 {@code null}（删除商品前可视为无库存、允许删除）；
     * 其它业务失败仍抛 {@link BusinessException}。
     * </p>
     *
     * @param goodsId 商品 ID
     * @return 快照 Map，或记录不存在时返回 null
     */
    public Map<String, Object> getUsable(Long goodsId) {
        return getStockSnapshot(goodsId);
    }

    /**
     * 从快照中解析可用数量（usableStock）；记录不存在时抛业务异常。
     * <p>
     * 删除场景请优先用 {@link #getStockSnapshot}，以便区分「无记录」与「有库存」。
     * </p>
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
     * 查询库存快照（至少含 stock / lockStock / usableStock）。
     * <p>
     * 用于删除商品前校验：若返回 null（库存记录不存在）可视为可删；
     * 若 {@code stock &gt; 0} 则不允许删除。
     * </p>
     *
     * @param goodsId 商品 ID
     * @return 快照；库存记录不存在时返回 null
     */
    public Map<String, Object> getStockSnapshot(Long goodsId) {
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
        } catch (RestClientException ex) {
            log.error("【库存远程】查询库存快照 HTTP 失败 goodsId={}", goodsId, ex);
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "调用库存服务失败（查询库存快照）：" + ex.getMessage());
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
