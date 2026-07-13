package com.inventory.modules.order.concurrency.v3;

import com.inventory.common.client.dto.LockStockFlowContext;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import com.inventory.order.client.InventoryStockClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * V3：线程池 + 任务拆分 + 异步处理（性能向改造，<strong>不替代</strong>并发安全方案）。
 * <p>
 * <b>设计要点</b>：
 * <ol>
 *   <li><strong>并发安全</strong>：沿用 V2，对 {@code goodsId} 使用 {@link ReentrantLock} 串行「读库存→建单→改 lock_stock」</li>
 *   <li><strong>同步核心</strong>：{@link OrderCreateConcurrencyV3SyncService} 完成校验、建单、lock_stock 更新</li>
 *   <li><strong>异步副路径</strong>：远程写流水、模拟通知提交到专用线程池，HTTP 可更早返回</li>
 * </ol>
 * </p>
 * <p>
 * <b>微服务适配</b>：异步写流水改为 {@link InventoryStockClient#writeFlow}，不再注入 StockService。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV3 implements OrderCreateConcurrencyStrategy {

    /** V3 同步核心（带 @Transactional，仅覆盖本地订单落库） */
    private final OrderCreateConcurrencyV3SyncService syncService;

    /** 异步写流水走远程库存客户端 */
    private final InventoryStockClient inventoryStockClient;

    /**
     * 与 V2 相同：按商品维度 JVM 内互斥锁。
     * 线程池<strong>不能</strong>替代此锁，否则仍会超锁。
     */
    private static final ConcurrentHashMap<Long, ReentrantLock> GOODS_LOCKS = new ConcurrentHashMap<>();

    /**
     * V3 专用异步线程池（演示用）。
     * <ul>
     *   <li>core=4：常驻 worker，处理流水/通知</li>
     *   <li>max=16：峰值可扩</li>
     *   <li>queue=2000：缓冲突发异步任务</li>
     *   <li>CallerRunsPolicy：队列满时由调用线程执行，避免静默丢弃</li>
     * </ul>
     */
    private ExecutorService asyncExecutor;

    @PostConstruct
    void initAsyncExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "v3-async-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        asyncExecutor = new ThreadPoolExecutor(
                4,
                16,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("[V3] 异步线程池已启动 core=4 max=16 queue=2000");
    }

    @PreDestroy
    void shutdownAsyncExecutor() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("[V3] 异步线程池已关闭");
        }
    }

    private static ReentrantLock lockForGoods(Long goodsId) {
        return GOODS_LOCKS.computeIfAbsent(goodsId, id -> new ReentrantLock());
    }

    @Override
    public String version() {
        return ConcurrencyVersion.V3.getCode();
    }

    /**
     * V3 创建订单入口。
     * <pre>
     * 主线程（Tomcat/JUnit）：
     *   lock(goodsId)
     *     → syncService 同步：校验 + 建单 + lock_stock
     *     → submit 异步：远程写流水 + 模拟通知
     *     → 立即 return 成功
     *   unlock
     * </pre>
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        if (dto == null || dto.getGoodsId() == null) {
            return Result.fail("goodsId 不能为空");
        }

        ReentrantLock lock = lockForGoods(dto.getGoodsId());
        lock.lock();
        try {
            V3SyncCreateResult syncResult;
            try {
                syncResult = syncService.syncCreateOrderAndLockStock(dto);
            } catch (IllegalStateException | BusinessException ex) {
                return Result.fail(ex.getMessage());
            }

            submitAsyncSideEffects(syncResult);

            return Result.success("订单创建成功，库存已锁定（V3：流水异步写入中）");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将非核心步骤提交到线程池。
     */
    private void submitAsyncSideEffects(V3SyncCreateResult syncResult) {
        asyncExecutor.execute(() -> runAsyncSideEffects(syncResult));
    }

    /**
     * 异步任务体：运行在 {@code v3-async-n} 线程，远程写流水。
     */
    private void runAsyncSideEffects(V3SyncCreateResult syncResult) {
        LockStockFlowContext ctx = syncResult.getFlowContext();
        String orderNo = syncResult.getOrderNo();
        try {
            // 操作类型 3 = 锁定库存
            inventoryStockClient.writeFlow(
                    ctx.getGoodsId(),
                    ctx.getGoodsName(),
                    ctx.getBeforeLockStock(),
                    ctx.getChangeQty(),
                    ctx.getAfterLockStock(),
                    3,
                    orderNo,
                    "V3异步写入：订单预占锁定库存"
            );
            log.debug("[V3异步] 库存流水已写入 orderNo={} goodsId={}", orderNo, ctx.getGoodsId());
        } catch (Exception ex) {
            // 同步路径已成功：此处失败需 V7 补偿任务兜底；实验版仅打日志
            log.error("[V3异步] 写库存流水失败 orderNo={} goodsId={}", orderNo, ctx.getGoodsId(), ex);
        }

        try {
            log.info("[V3异步] 模拟发送下单通知 orderNo={} user={}", orderNo, syncResult.getUserName());
        } catch (Exception ex) {
            log.warn("[V3异步] 模拟通知失败 orderNo={}", orderNo, ex);
        }
    }
}
