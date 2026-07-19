package com.inventory.modules.order.concurrency.v5;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.concurrency.v1.OrderCreateConcurrencyV1SyncService;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * V5r 策略入口：流程与 {@link OrderCreateConcurrencyV5} 一模一样，只把锁换成 Redisson。
 * <p>
 * 读代码时：左右对照 V5 —— 步骤编号相同，换的是 tryLock / unlock 的实现类。
 * 压测：{@code ?version=v5r}
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV5Redisson implements OrderCreateConcurrencyStrategy {

    private final OrderCreateConcurrencyV1SyncService v1SyncService;
    private final GoodsStockRedissonLock goodsStockRedissonLock;

    /** 与手写 V5 共用等待时间配置 */
    @Value("${app.concurrency.v5.lock-wait-ms:30000}")
    private long lockWaitMs;

    /**
     * &lt;=0 看门狗；&gt;0 固定租约。默认 -1。
     */
    @Value("${app.concurrency.v5r.lock-lease-seconds:-1}")
    private long lockLeaseSeconds;

    @Override
    public String version() {
        return ConcurrencyVersion.V5R.getCode();
    }

    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        // 步骤 1：校验（同 V5）
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        // 步骤 2：Redisson 抢锁（同 V5 的 tryLock，实现不同）
        RLock lock = goodsStockRedissonLock.tryLock(dto.getGoodsId(), lockWaitMs, lockLeaseSeconds);
        if (lock == null) {
            return Result.fail("系统繁忙，获取商品库存锁超时（V5r Redisson），请稍后重试");
        }

        try {
            // 步骤 3：临界区业务完全相同
            String orderNo = v1SyncService.syncCreateOrderNonAtomic(dto);
            return Result.success("订单创建成功，库存已锁定（V5r：Redisson 看门狗锁，订单号 " + orderNo + "）");
        } catch (IllegalStateException | BusinessException ex) {
            return Result.fail(ex.getMessage());
        } finally {
            // 步骤 4：释放 Redisson 锁
            goodsStockRedissonLock.unlock(lock);
        }
    }
}
