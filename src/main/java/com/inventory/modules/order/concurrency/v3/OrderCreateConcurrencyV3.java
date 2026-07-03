package com.inventory.modules.order.concurrency.v3;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stock.dto.LockStockFlowContext;
import com.inventory.modules.invertory.stock.service.StockService;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
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
 *   <li><strong>同步核心</strong>：{@link OrderCreateConcurrencyV3SyncService} 在一个事务内完成校验、建单、lock_stock 更新</li>
 *   <li><strong>异步副路径</strong>：库存流水、模拟通知提交到专用线程池，HTTP 可更早返回</li>
 * </ol>
 * </p>
 * <p>
 * <b>与 V2 对比</b>：正确性指标相同（200 并发应 100 成功 / 不超锁）；
 * 期望 JMeter 平均 RT、吞吐略优于 V2（同步路径少做写流水 IO）。
 * </p>
 * <p>
 * <b>注意</b>：异步流水失败时订单与 lock_stock 已提交，需 V7 补偿；本版本仅实验演示。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConcurrencyV3 implements OrderCreateConcurrencyStrategy {

    /** V3 同步核心（带 @Transactional） */
    private final OrderCreateConcurrencyV3SyncService syncService;

    /** 异步写流水 */
    private final StockService stockService;

    /**
     * 与 V2 相同：按商品维度 JVM 内互斥锁。
     * 线程池<strong>不能</strong>替代此锁，否则仍会超锁。
     */
    private static final ConcurrentHashMap<Long, ReentrantLock> GOODS_LOCKS = new ConcurrentHashMap<>();

    /**
     * V3 专用异步线程池（演示用，生产应使用 Spring {@code @Async} + 统一线程池配置）。
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
     *     → submit 异步：写流水 + 模拟通知
     *     → 立即 return 成功
     *   unlock
     * 线程池 worker：
     *   → writeFlow / log 通知
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
            // ---------- 同步核心：事务提交后 lock_stock 与订单已一致 ----------
            V3SyncCreateResult syncResult;
            try {
                syncResult = syncService.syncCreateOrderAndLockStock(dto);
            } catch (IllegalStateException ex) {
                // 库存不足等业务失败，与 OrderInfoService 返回 fail 语义一致
                return Result.fail(ex.getMessage());
            }

            // ---------- 异步副路径：不阻塞 HTTP 响应（流水、通知） ----------
            submitAsyncSideEffects(syncResult);

            return Result.success("订单创建成功，库存已锁定（V3：流水异步写入中）");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将非核心步骤提交到线程池。
     * <p>
     * 在锁内 submit 仅是把任务放入队列，<strong>不会</strong>在 unlock 前执行完 worker 逻辑；
     * 因此接口 RT 不包含写流水耗时（与 V2 全同步 lockStock 对比）。
     * </p>
     */
    private void submitAsyncSideEffects(V3SyncCreateResult syncResult) {
        // 写法 A（当前使用）：Lambda，等价于 Runnable
        asyncExecutor.execute(() -> runAsyncSideEffects(syncResult));

        /*
         * 写法 B：匿名内部类，与写法 A 完全等价，不用 Lambda 时可改成下面这样（二选一，不要同时启用）：
         *
         * asyncExecutor.execute(new Runnable() {
         *     @Override
         *     public void run() {
         *         runAsyncSideEffects(syncResult);
         *     }
         * });
         *
         * 写法 C：单独 Runnable 类，任务很重、想拆文件时用（同样注释掉 A 再启用）：
         *
         * asyncExecutor.execute(new AsyncSideEffectsTask(syncResult));
         */
    }

    /*
     * 写法 C 配套的 Runnable 实现（默认注释，启用写法 C 时去掉本块注释）：
     *
     * private final class AsyncSideEffectsTask implements Runnable {
     *
     *     private final V3SyncCreateResult syncResult;
     *
     *     AsyncSideEffectsTask(V3SyncCreateResult syncResult) {
     *         this.syncResult = syncResult;
     *     }
     *
     *     @Override
     *     public void run() {
     *         runAsyncSideEffects(syncResult);
     *     }
     * }
     */

    /**
     * 异步任务体：运行在 {@code v3-async-n} 线程，拥有独立栈帧，共享堆上 Bean。
     */
    private void runAsyncSideEffects(V3SyncCreateResult syncResult) {
        LockStockFlowContext ctx = syncResult.getFlowContext();
        String orderNo = syncResult.getOrderNo();
        try {
            // 操作类型 3 = 锁定库存（与 StockServiceImpl.lockStock 一致）
            stockService.writeFlow(
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
            // 模拟：发短信/站内信等非核心 IO，真实项目可对接 MQ 或通知服务
            log.info("[V3异步] 模拟发送下单通知 orderNo={} user={}", orderNo, syncResult.getUserName());
        } catch (Exception ex) {
            log.warn("[V3异步] 模拟通知失败 orderNo={}", orderNo, ex);
        }
    }
}
