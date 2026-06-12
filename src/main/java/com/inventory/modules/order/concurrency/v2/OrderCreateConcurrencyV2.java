package com.inventory.modules.order.concurrency.v2;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.modules.order.orderinfo.service.OrderInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * V2：JUC 基础并发（ReentrantLock 按商品维度互斥）。
 * <p>
 * <b>实现说明</b>：在调用 {@link OrderInfoService#createOrder} 外包一层
 * {@code ReentrantLock}，保证<strong>同一 goodsId 在同一 JVM 内</strong>
 * 「读可用库存 → 建单 → lockStock」串行执行，避免 V1 的读-写竞态。
 * </p>
 * <p>
 * <b>压测目标</b>：200 并发下成功订单数 ≤ stock（100），lockStock 不超过 stock。
 * </p>
 * <p>
 * <b>局限</b>：锁在 JVM 内存中，<strong>多实例部署无效</strong>，需 V5 Redis 或 V4 SQL。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV2 implements OrderCreateConcurrencyStrategy {

    private final OrderInfoService orderInfoService;

    /**
     * 按商品 ID 维度的可重入锁；压测场景下 computeIfAbsent 即可。
     */
    private static final ConcurrentHashMap<Long, ReentrantLock> GOODS_LOCKS = new ConcurrentHashMap<>();

    /**
     * 获取指定商品的 JVM 内锁对象（不存在则创建）。
     */
    private static ReentrantLock lockForGoods(Long goodsId) {
        return GOODS_LOCKS.computeIfAbsent(goodsId, id -> new ReentrantLock());
    }

    @Override
    public String version() {
        return ConcurrencyVersion.V2.getCode();
    }

    /**
     * V2 创建订单：按 goodsId 加 ReentrantLock，再委托现有 createOrder 逻辑。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        ReentrantLock lock = lockForGoods(dto.getGoodsId());
        lock.lock();
        try {
            return orderInfoService.createOrder(dto);
        } finally {
            lock.unlock();
        }
    }
}
