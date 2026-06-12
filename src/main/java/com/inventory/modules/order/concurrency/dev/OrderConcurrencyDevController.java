package com.inventory.modules.order.concurrency.dev;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyTestConstants;
import com.inventory.modules.order.concurrency.common.ConcurrencyTestHelper;
import com.inventory.modules.order.concurrency.common.vo.ConcurrencyStockResultVO;
import com.inventory.modules.order.concurrency.facade.OrderConcurrencyFacade;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 并发压测专用接口（仅 dev 环境加载）。
 * <p>
 * <b>说明</b>：
 * <ul>
 *   <li>使用 {@code @Profile("dev")}，生产 {@code prd} 环境不会注册本 Controller</li>
 *   <li>压测需携带登录 Token（与正式接口相同），除非后续加入 Security 白名单</li>
 *   <li>正式业务仍走 {@code POST /order/info/add}；实验走本类 {@code /order/concurrency/**}</li>
 * </ul>
 * </p>
 *
 * @author 95349
 */
@RestController
@RequestMapping("/order/concurrency")
@Profile("dev")
@Validated
public class OrderConcurrencyDevController {

    private final OrderConcurrencyFacade orderConcurrencyFacade;
    private final ConcurrencyTestHelper concurrencyTestHelper;

    public OrderConcurrencyDevController(OrderConcurrencyFacade orderConcurrencyFacade,
                                       ConcurrencyTestHelper concurrencyTestHelper) {
        this.orderConcurrencyFacade = orderConcurrencyFacade;
        this.concurrencyTestHelper = concurrencyTestHelper;
    }

    /**
     * 并发实验：创建订单（锁库存）。
     * <p>
     * 示例：{@code POST /api/order/concurrency/order/add?version=v1}
     * </p>
     *
     * @param version 并发版本 v1～v7
     * @param dto     与正式新增订单相同
     * @return 下单结果
     */
    @PostMapping("/order/add")
    public Result<?> addOrder(
            @RequestParam(defaultValue = "v1") String version,
            @Valid @RequestBody OrderInfoDTO dto) {
        return orderConcurrencyFacade.createOrder(version, dto);
    }

    /**
     * 压测前重置商品库存与锁定库存。
     * <p>
     * 示例：{@code POST /api/order/concurrency/stock/reset?goodsId=2064625692771397632&stock=100&lockStock=0}
     * </p>
     *
     * @param goodsId   商品 ID，默认压测商品小米17
     * @param stock     当前库存，默认 100
     * @param lockStock 锁定库存，默认 0
     */
    @PostMapping("/stock/reset")
    public Result<Void> resetStock(
            @RequestParam(required = false) Long goodsId,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) Integer lockStock) {
        Long targetGoodsId = goodsId != null ? goodsId : ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID;
        int targetStock = stock != null ? stock : ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK;
        int targetLockStock = lockStock != null ? lockStock : ConcurrencyTestConstants.DEFAULT_INITIAL_LOCK_STOCK;
        return concurrencyTestHelper.resetStock(targetGoodsId, targetStock, targetLockStock);
    }

    /**
     * 压测后查询库存快照（stock、lockStock、待支付订单数、是否超锁）。
     * <p>
     * 示例：{@code GET /api/order/concurrency/stock/result?goodsId=2064625692771397632}
     * </p>
     */
    @GetMapping("/stock/result")
    public Result<ConcurrencyStockResultVO> stockResult(
            @RequestParam(required = false) Long goodsId) {
        Long targetGoodsId = goodsId != null ? goodsId : ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID;
        return concurrencyTestHelper.queryStockResult(targetGoodsId);
    }

    /**
     * 列出已注册的并发版本（v1～v7 中已加载的 Spring Bean）。
     */
    @GetMapping("/versions")
    public Result<List<String>> listVersions() {
        return orderConcurrencyFacade.listVersions();
    }
}
