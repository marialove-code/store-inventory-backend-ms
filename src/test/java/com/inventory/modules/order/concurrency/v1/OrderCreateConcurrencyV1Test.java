package com.inventory.modules.order.concurrency.v1;

import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import com.inventory.modules.order.concurrency.common.ConcurrencyTestConstants;
import com.inventory.modules.order.concurrency.common.ConcurrencyTestHelper;
import com.inventory.modules.order.concurrency.common.vo.ConcurrencyStockResultVO;
import com.inventory.modules.order.concurrency.facade.OrderConcurrencyFacade;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V1 并发压测 — JUnit 集成测试。
 * <p>
 * <b>运行前准备</b>：
 * <ul>
 *   <li>本地 PostgreSQL、Redis 可连（与 {@code application-dev.yml} 一致）</li>
 *   <li>环境变量 {@code DB_PWD}、{@code REDIS_PWD} 已配置（IDEA Run Configuration 或系统环境）</li>
 *   <li>压测商品 {@link ConcurrencyTestConstants#DEFAULT_TEST_GOODS_ID} 在库中存在</li>
 *   <li>建议压测前手动取消/清理该商品历史待支付订单，避免 lockStock 干扰</li>
 * </ul>
 * </p>
 * <p>
 * <b>V1 预期</b>：高并发下可能出现超锁（成功数 &gt; 100 或 lockStock &gt; stock），
 * 本测试不断言「必须超锁」，而是输出指标供写入 {@code docs/并发演进.md}。
 * </p>
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
@Tag("integration")
@DisplayName("V1 并发：新增订单锁库存")
class OrderCreateConcurrencyV1Test {

    /** 并发线程数，与文档计划一致；本机较慢时可改为 50～100 */
    private static final int CONCURRENT_THREADS = 200;

    /** 所有线程同时起跑的信号 */
    private static final int STARTUP_TIMEOUT_SECONDS = 120;

    @Autowired
    private OrderConcurrencyFacade orderConcurrencyFacade;

    @Autowired
    private ConcurrencyTestHelper concurrencyTestHelper;

    /**
     * 每个用例前重置库存，保证起点：stock=100，lockStock=0。
     */
    @BeforeEach
    void resetStockBeforeTest() {
        Result<Void> resetResult = concurrencyTestHelper.resetStock(
                ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID,
                ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK,
                ConcurrencyTestConstants.DEFAULT_INITIAL_LOCK_STOCK
        );
        assertEquals(ResultCode.SUCCESS.getCode(), resetResult.getCode(),
                "压测前重置库存失败: " + resetResult.getMessage());
    }

    /**
     * 冒烟：单线程下一单应成功（验证环境与 V1 基线可用）。
     */
    @Test
    @DisplayName("冒烟：单笔下单成功")
    void smoke_singleCreateOrder_success() {
        Result<?> result = orderConcurrencyFacade.createOrder("v1", buildOrderDto("smoke"));
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode(), result.getMessage());

        Result<ConcurrencyStockResultVO> snapshot = concurrencyTestHelper.queryStockResult(
                ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        assertEquals(ResultCode.SUCCESS.getCode(), snapshot.getCode());
        assertNotNull(snapshot.getData());
        log.info("冒烟结果: lockStock={}, usableStock={}",
                snapshot.getData().getLockStock(), snapshot.getData().getUsableStock());
    }

    /**
     * V1 核心：多线程同时调用 {@code version=v1} 创建订单，统计成功/失败与最终 lockStock。
     * <p>
     * 运行结束后请将控制台输出填入 {@code docs/并发演进.md} 的 V1 行。
     * </p>
     */
    @Test
    @DisplayName("V1：200 并发下单，输出超锁指标")
    void v1_concurrentCreateOrder_reportMetrics() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        try {
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int index = i;
                pool.submit(() -> {
                    try {
                        // 等待统一发令，尽量同时撞车
                        if (index == 0) {
                            log.info("[并发调试] 子线程在 startGate.await() 等待, 线程名={}, index={}",
                                    Thread.currentThread().getName(), index);
                        }
                        startGate.await();
                        if (index == 0) {
                            log.info("[并发调试] 子线程已放行, 即将 createOrder, 线程名={}",
                                    Thread.currentThread().getName());
                        }
                        Result<?> result = orderConcurrencyFacade.createOrder("v1", buildOrderDto("t" + index));
                        if (result.getCode() == ResultCode.SUCCESS.getCode()) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception ex) {
                        failCount.incrementAndGet();
                        log.debug("并发下单异常: {}", ex.getMessage());
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            // 发令
            log.info("[并发调试] 主线程 startGate.countDown() 发令, 线程名={}",
                    Thread.currentThread().getName());
            startGate.countDown();
            boolean finished = doneGate.await(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertTrue(finished, "并发任务在超时时间内未全部结束，可调大 STARTUP_TIMEOUT_SECONDS 或减小 CONCURRENT_THREADS");
        } finally {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        Result<ConcurrencyStockResultVO> snapshotResult = concurrencyTestHelper.queryStockResult(
                ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        assertEquals(ResultCode.SUCCESS.getCode(), snapshotResult.getCode());
        ConcurrencyStockResultVO snapshot = snapshotResult.getData();
        assertNotNull(snapshot);

        int success = successCount.get();
        int fail = failCount.get();
        int lockStock = snapshot.getLockStock() == null ? 0 : snapshot.getLockStock();
        int stock = snapshot.getStock() == null ? 0 : snapshot.getStock();
        boolean overLocked = Boolean.TRUE.equals(snapshot.getOverLocked());

        // ===== 控制台输出（复制到 docs/并发演进.md）=====
        log.info("========== V1 并发压测结果 ==========");
        log.info("并发线程数: {}", CONCURRENT_THREADS);
        log.info("成功下单数: {}", success);
        log.info("失败次数:   {}", fail);
        log.info("当前库存 stock:     {}", stock);
        log.info("锁定库存 lockStock: {}", lockStock);
        log.info("可用库存 usable:    {}", snapshot.getUsableStock());
        log.info("待支付订单数:       {}", snapshot.getPendingOrderCount());
        log.info("是否疑似超锁:       {}", overLocked);
        log.info("=====================================");

        // 基本完整性：每个线程都有结果
        assertEquals(CONCURRENT_THREADS, success + fail, "成功+失败应等于并发数");

        // V1 文档化提示（不断言必须超锁，避免环境差异导致测试红）
        if (success > ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK || overLocked) {
            log.warn("V1 已复现超锁/超额成功：成功数={}，lockStock={}，可写入文档", success, lockStock);
        } else {
            log.warn("V1 本次未明显超锁，可尝试：增大 CONCURRENT_THREADS、或临时在锁库存处加 sleep 放大竞态");
        }
    }

    /**
     * 构造与前端新增订单一致的 DTO。
     *
     * @param tag 用户名后缀，便于区分并发订单
     */
    private OrderInfoDTO buildOrderDto(String tag) {
        OrderInfoDTO dto = new OrderInfoDTO();
        dto.setUserName("junit-" + tag);
        dto.setGoodsId(ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        dto.setGoodsName("小米17");
        dto.setCostPrice(new BigDecimal("3999"));
        dto.setSalePrice(new BigDecimal("4399"));
        dto.setBuyQty(ConcurrencyTestConstants.DEFAULT_BUY_QTY);
        dto.setRemark("V1并发JUnit");
        return dto;
    }
}
