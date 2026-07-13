package com.inventory.modules.order.concurrency.v4;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V4 并发压测 — JUnit 集成测试。
 * <p>
 * 与 V2 相同 200 线程模型；区别为 {@code version=v4}（SQL 原子条件，无 JVM 锁）。
 * V4 流水同步写入，无需像 V3 那样额外 sleep 等待异步任务。
 * </p>
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
@Tag("integration")
@DisplayName("V4 并发：SQL 原子条件锁库存")
class OrderCreateConcurrencyV4Test {

    private static final int CONCURRENT_THREADS = 200;
    private static final int STARTUP_TIMEOUT_SECONDS = 120;
    private static final String VERSION = "v4";

    @Autowired
    private OrderConcurrencyFacade orderConcurrencyFacade;

    @Autowired
    private ConcurrencyTestHelper concurrencyTestHelper;

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

    @Test
    @DisplayName("冒烟：V4 单笔下单成功")
    void smoke_singleCreateOrder_success() {
        Result<?> result = orderConcurrencyFacade.createOrder(VERSION, buildOrderDto("smoke"));
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode(), result.getMessage());
    }

    @Test
    @DisplayName("V4：200 并发下单，应不超锁")
    void v4_concurrentCreateOrder_shouldNotOverLock() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        try {
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int index = i;
                pool.submit(new ConcurrentOrderTask(
                        startGate,
                        doneGate,
                        successCount,
                        failCount,
                        index
                ));
            }
            startGate.countDown();
            assertTrue(doneGate.await(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "并发任务在超时时间内未全部结束");
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

        log.info("========== V4 并发压测结果 ==========");
        log.info("并发线程数: {}", CONCURRENT_THREADS);
        log.info("成功下单数: {}", success);
        log.info("失败次数:   {}", fail);
        log.info("当前库存 stock:     {}", stock);
        log.info("锁定库存 lockStock: {}", lockStock);
        log.info("可用库存 usable:    {}", snapshot.getUsableStock());
        log.info("待支付订单数:       {}", snapshot.getPendingOrderCount());
        log.info("是否疑似超锁:       {}", overLocked);
        log.info("=====================================");

        assertEquals(CONCURRENT_THREADS, success + fail);
        assertTrue(success <= ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK,
                "V4 成功数应不超过初始库存 100，实际=" + success);
        assertTrue(lockStock <= stock, "V4 lockStock 应不超过 stock");
        assertFalse(overLocked, "V4 不应出现超锁");
    }

    private OrderInfoDTO buildOrderDto(String tag) {
        OrderInfoDTO dto = new OrderInfoDTO();
        dto.setUserName("junit-" + tag);
        dto.setGoodsId(ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        dto.setGoodsName("小米17");
        dto.setCostPrice(new BigDecimal("3999"));
        dto.setSalePrice(new BigDecimal("4399"));
        dto.setBuyQty(ConcurrencyTestConstants.DEFAULT_BUY_QTY);
        dto.setRemark("V4并发JUnit");
        return dto;
    }

    /**
     * 单个并发下单任务（不用 Lambda，便于阅读线程内逻辑）。
     */
    private class ConcurrentOrderTask implements Runnable {

        private final CountDownLatch startGate;
        private final CountDownLatch doneGate;
        private final AtomicInteger successCount;
        private final AtomicInteger failCount;
        private final int index;

        ConcurrentOrderTask(CountDownLatch startGate,
                              CountDownLatch doneGate,
                              AtomicInteger successCount,
                              AtomicInteger failCount,
                              int index) {
            this.startGate = startGate;
            this.doneGate = doneGate;
            this.successCount = successCount;
            this.failCount = failCount;
            this.index = index;
        }

        @Override
        public void run() {
            try {
                startGate.await();
                Result<?> result = orderConcurrencyFacade.createOrder(
                        VERSION,
                        buildOrderDto("t" + index)
                );
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
        }
    }
}
