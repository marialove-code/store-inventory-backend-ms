package com.inventory.modules.order.concurrency.v3;

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
 * V3 并发压测 — JUnit 集成测试。
 * <p>
 * 与 V2 相同线程模型；区别为 {@code version=v3}（同步核心 + 异步流水）。
 * 断言：不超锁；压测后稍等片刻再查流水（异步写入）。
 * </p>
 */
@Slf4j // 本类可用 log.info / log.debug 打日志
@SpringBootTest // 启动 Spring Boot，测试里可以 @Autowired 真实 Service
@ActiveProfiles("dev") // 使用 dev 环境配置（默认加载 concurrency Controller）
@Tag("integration") // 标记为集成测试：依赖 PostgreSQL + inventory-service HTTP
@DisplayName("V3 并发：线程池 + ReentrantLock") // IDEA 测试窗口里显示的名称
class OrderCreateConcurrencyV3Test {

    /** 并发线程数：200 个线程同时抢 100 件库存 */
    private static final int CONCURRENT_THREADS = 200;
    /** 等待所有并发任务结束的最长时间（秒），超时则测试失败 */
    private static final int STARTUP_TIMEOUT_SECONDS = 120;
    /** V3 异步写流水需要额外等待的秒数（主流程返回后流水还在后台写） */
    private static final int ASYNC_DRAIN_SECONDS = 5;
    /** 调用门面时传入的版本号，对应 OrderCreateConcurrencyV3 */
    private static final String VERSION = "v3";

    /** 并发实验统一入口：createOrder("v3", dto) 会走到 V3 实现 */
    @Autowired
    private OrderConcurrencyFacade orderConcurrencyFacade;

    /** 压测辅助：重置库存、查询 stock/lockStock/是否超锁 */
    @Autowired
    private ConcurrencyTestHelper concurrencyTestHelper;

    /**
     * 每个测试方法运行前都会执行：把压测商品库存重置为 100、锁定库存归零。
     * 避免上次测试残留数据导致「库存不足」或误判超锁。
     */
    @BeforeEach
    void resetStockBeforeTest() {
        // 调用辅助接口，把小米17 的 stock=100、lockStock=0 写回数据库
        Result<Void> resetResult = concurrencyTestHelper.resetStock(
                ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID,      // 2064625692771397632
                ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK,      // 100
                ConcurrencyTestConstants.DEFAULT_INITIAL_LOCK_STOCK  // 0
        );
        // 重置必须成功，否则后面压测没有意义
        assertEquals(ResultCode.SUCCESS.getCode(), resetResult.getCode(),
                "压测前重置库存失败: " + resetResult.getMessage());
    }

    /**
     * 冒烟测试：只发 1 笔订单，确认 V3 链路能跑通（不测 200 并发）。
     */
    @Test
    @DisplayName("冒烟：V3 单笔下单成功")
    void smoke_singleCreateOrder_success() throws InterruptedException {
        // 走 V3 创建一笔订单，用户名 junit-smoke
        Result<?> result = orderConcurrencyFacade.createOrder(VERSION, buildOrderDto("smoke"));
        // 期望业务 code=200 表示下单成功
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode(), result.getMessage());
        // V3 流水是异步写的，睡 5 秒让后台线程池写完 inventory_flow
        Thread.sleep(ASYNC_DRAIN_SECONDS * 1000L);
    }

    /**
     * 核心压测：200 线程同时下单，验证 V3 不会超锁（成功数 ≤100，lockStock ≤ stock）。
     */
    @Test
    @DisplayName("V3：200 并发下单，应不超锁")
    void v3_concurrentCreateOrder_shouldNotOverLock() throws InterruptedException {
        // startGate 初始计数 1：所有工作线程在 await 处阻塞，直到主线程 countDown
        CountDownLatch startGate = new CountDownLatch(1);
        // doneGate 初始计数 200：每完成一个任务 countDown 一次，减到 0 表示全部跑完
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_THREADS);
        // 线程安全计数：下单成功次数（code=200）
        AtomicInteger successCount = new AtomicInteger();
        // 线程安全计数：下单失败次数（库存不足或异常）
        AtomicInteger failCount = new AtomicInteger();

        // 创建 200 个线程的固定线程池（模拟 200 并发用户）
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        try {
            // 循环 200 次，每次往线程池提交一个下单任务
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                // lambda 里只能引用 final 或 effectively final 变量，所以复制 i
                final int index = i;
                pool.submit(() -> {
                    try {
                        // 所有线程在此等待，直到主线程打开 startGate（统一起跑，模拟 JMeter 同时压）
                        startGate.await();
                        // 调用 V3 下单，每个线程用不同用户名 junit-t0 … junit-t199
                        Result<?> result = orderConcurrencyFacade.createOrder(VERSION, buildOrderDto("t" + index));
                        if (result.getCode() == ResultCode.SUCCESS.getCode()) {
                            successCount.incrementAndGet(); // 成功 +1
                        } else {
                            failCount.incrementAndGet();  // 业务失败 +1
                        }
                    } catch (Exception ex) {
                        failCount.incrementAndGet(); // 抛异常也算失败
                        log.debug("并发下单异常: {}", ex.getMessage());
                    } finally {
                        doneGate.countDown(); // 无论成功失败，都通知「我这一单跑完了」
                    }
                });
            }
            // 主线程减 1，startGate 变 0，200 个线程同时开始下单
            startGate.countDown();
            // 最多等 120 秒，直到 doneGate 被减到 0；否则断言失败
            assertTrue(doneGate.await(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "并发任务在超时时间内未全部结束");
        } finally {
            // 关闭线程池，不再接受新任务
            pool.shutdown();
            // 最多再等 30 秒让已提交任务执行完
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        // V3 特点：HTTP/同步路径已返回，但 inventory_flow 可能还在 v3-async 线程池里写
        log.info("等待 V3 异步流水 {} 秒...", ASYNC_DRAIN_SECONDS);
        Thread.sleep(ASYNC_DRAIN_SECONDS * 1000L);

        // 从数据库查当前库存快照（等同 GET /order/concurrency/stock/result）
        Result<ConcurrencyStockResultVO> snapshotResult = concurrencyTestHelper.queryStockResult(
                ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        assertEquals(ResultCode.SUCCESS.getCode(), snapshotResult.getCode());
        ConcurrencyStockResultVO snapshot = snapshotResult.getData();
        assertNotNull(snapshot);

        // 取出内存里统计的成功/失败次数
        int success = successCount.get();
        int fail = failCount.get();
        // 锁定库存；null 当 0 处理
        int lockStock = snapshot.getLockStock() == null ? 0 : snapshot.getLockStock();
        // 当前总库存
        int stock = snapshot.getStock() == null ? 0 : snapshot.getStock();
        // 是否超锁：lockStock > stock 等逻辑在 Helper 里算好
        boolean overLocked = Boolean.TRUE.equals(snapshot.getOverLocked());

        // 打印汇总，方便和 JMeter、并发演进.md 对照
        log.info("========== V3 并发压测结果 ==========");
        log.info("并发线程数: {}", CONCURRENT_THREADS);
        log.info("成功下单数: {}", success);
        log.info("失败次数:   {}", fail);
        log.info("当前库存 stock:     {}", stock);
        log.info("锁定库存 lockStock: {}", lockStock);
        log.info("可用库存 usable:    {}", snapshot.getUsableStock());
        log.info("待支付订单数:       {}", snapshot.getPendingOrderCount());
        log.info("是否疑似超锁:       {}", overLocked);
        log.info("=====================================");

        // 200 个线程各执行一次，成功+失败必须等于 200
        assertEquals(CONCURRENT_THREADS, success + fail);
        // 只有 100 件货，成功数不能超过 100（V3 与 V2 一样靠锁保证）
        assertTrue(success <= ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK,
                "V3 成功数应不超过初始库存 100，实际=" + success);
        // 锁定库存不能超过总库存
        assertTrue(lockStock <= stock, "V3 lockStock 应不超过 stock");
        // 最关键：不能出现 V1 那种 109 成功、超锁 9 件
        assertFalse(overLocked, "V3 不应出现超锁");
    }

    /**
     * 构造一笔下单请求体，与 Postman/JMeter 的 JSON 字段一致。
     *
     * @param tag 用户名后缀，如 smoke → junit-smoke，t0 → junit-t0
     */
    private OrderInfoDTO buildOrderDto(String tag) {
        OrderInfoDTO dto = new OrderInfoDTO();
        dto.setUserName("junit-" + tag); // 压测订单用户名，便于 SQL 清理
        dto.setGoodsId(ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID); // 小米17
        dto.setGoodsName("小米17");
        dto.setCostPrice(new BigDecimal("3999"));
        dto.setSalePrice(new BigDecimal("4399"));
        dto.setBuyQty(ConcurrencyTestConstants.DEFAULT_BUY_QTY); // 每单买 1 件
        dto.setRemark("V3并发JUnit");
        return dto;
    }
}
