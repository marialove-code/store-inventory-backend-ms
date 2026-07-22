package com.inventory.modules.order.concurrency.v2;

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
 * V2 并发压测 — JUnit 集成测试。
 * <p>
 * <b>与 V1 测试的关系</b>：线程模型、Latch、线程池用法与 {@code OrderCreateConcurrencyV1Test} 相同，
 * 区别仅在于调用 {@code version=v2}，且 V2 会断言「不应超锁」。
 * </p>
 * <p>
 * <b>V2 业务含义</b>：{@link OrderCreateConcurrencyV2} 在 {@code createOrder} 外包了
 * 按 {@code goodsId} 维度的 {@code ReentrantLock}，同一 JVM 内同一商品下单串行，
 * 避免 V1「读库存 → 判断 → 写单 → lockStock」的竞态。
 * </p>
 * <p>
 * <b>运行前准备</b>（与 V1 相同）：
 * <ul>
 *   <li>本地 PostgreSQL 可连；需同时启动 inventory-service（8082）</li>
 *   <li>环境变量 {@code DB_PWD} 已配置</li>
 *   <li>压测商品 {@link ConcurrencyTestConstants#DEFAULT_TEST_GOODS_ID} 在库中存在</li>
 *   <li>建议清理该商品历史 {@code junit-*} 待支付订单，避免 lockStock 干扰</li>
 * </ul>
 * </p>
 * <p>
 * <b>V2 预期</b>：200 并发下成功数 ≤ 100，lockStock ≤ stock，{@code overLocked=false}。
 * 与 V1（约 109 成功、超锁 9 单）形成对比，结果写入 {@code docs/并发/01-压测数据.md}。
 * </p>
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
@Tag("integration")
@DisplayName("V2 并发：ReentrantLock 锁库存")
class OrderCreateConcurrencyV2Test {

    /**
     * 并发线程数，与 V1、文档计划一致。
     * 条件相同才便于对比 V1 超锁 vs V2 不超锁。
     */
    private static final int CONCURRENT_THREADS = 200;

    /**
     * 主线程等待 200 个子任务全部结束的最长时间（秒）。
     * 若超时，说明 DB/线程可能卡住，可调大或减小 {@link #CONCURRENT_THREADS}。
     */
    private static final int STARTUP_TIMEOUT_SECONDS = 120;

    /**
     * 压测路由版本号，对应 {@code POST .../order/add?version=v2}。
     * Facade 会路由到 {@link OrderCreateConcurrencyV2}。
     */
    private static final String VERSION = "v2";

    /**
     * 并发实验统一门面：根据 {@code VERSION} 选择 V2 策略实现。
     * 由 Spring 注入单例 Bean。
     */
    @Autowired
    private OrderConcurrencyFacade orderConcurrencyFacade;

    /**
     * 压测辅助：重置 stock/lockStock、查询压测结果快照。
     */
    @Autowired
    private ConcurrencyTestHelper concurrencyTestHelper;

    /**
     * 每个测试方法执行前重置库存，保证起点一致：stock=100，lockStock=0。
     * <p>
     * 注意：只重置 {@code inventory_stock}，不会删除历史测试订单；
     * 若 lockStock 对不上，请手动 DELETE {@code user_name LIKE 'junit-%'} 的订单。
     * </p>
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
     * 冒烟测试：单线程、单次调用 V2，确认环境与路由正常。
     * <p>
     * 此时只有 {@code main} 线程，无线程池，用于排除「连不通 DB / V2 Bean 未注册」等问题，
     * 再跑 200 并发压测。
     * </p>
     */
    @Test
    @DisplayName("冒烟：V2 单笔下单成功")
    void smoke_singleCreateOrder_success() {
        // version=v2 → OrderCreateConcurrencyV2 → ReentrantLock → OrderInfoService.createOrder
        Result<?> result = orderConcurrencyFacade.createOrder(VERSION, buildOrderDto("smoke"));
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode(), result.getMessage());
    }

    /**
     * V2 核心压测：200 线程同时调用 {@code version=v2} 创建订单。
     * <p>
     * <b>执行线程说明</b>：
     * <ul>
     *   <li><b>主线程 main</b>：创建 Latch/线程池 → for 循环 submit → 发令 → await 等待 → 查库 → 断言</li>
     *   <li><b>子线程 pool-x</b>：await 等发令 → createOrder → 统计成功/失败 → doneGate.countDown()</li>
     * </ul>
     * for 循环里只有 {@code pool.submit} 在 main 执行；{@code createOrder} 在子线程执行。
     * </p>
     * <p>
     * <b>与 V1 断言差异</b>：V1 不断言必须超锁（只输出指标）；V2 断言成功 ≤ 100 且 lockStock ≤ stock。
     * </p>
     */
    @Test
    @DisplayName("V2：200 并发下单，应不超锁")
    void v2_concurrentCreateOrder_shouldNotOverLock() throws InterruptedException {

        // ==================== 1. 准备并发工具（main 线程执行） ====================

        /**
         * 发令门闩：初始计数 1。
         * 200 个子线程在 startGate.await() 阻塞；main 执行 countDown() 后同时放行。
         * 作用：尽量让 200 个 createOrder 在同一时刻撞 V2 的 ReentrantLock。
         */
        CountDownLatch startGate = new CountDownLatch(1);

        /**
         * 完成门闩：初始计数 200。
         * 每个子线程在 finally 里 countDown() 一次；main 在 doneGate.await() 等待全部完成后再查库。
         * 作用：保证统计完 200 次结果再读 lockStock。
         */
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_THREADS);

        /**
         * 线程安全成功计数。多个子线程同时 increment，必须用 AtomicInteger，不能用 int。
         */
        AtomicInteger successCount = new AtomicInteger();

        /** 线程安全失败计数（含业务 fail 与异常）。 */
        AtomicInteger failCount = new AtomicInteger();

        /**
         * 固定 200 线程的线程池：最多 200 个任务并行。
         * 与 CONCURRENT_THREADS 一致，避免任务在队列里排队导致「假串行」。
         */
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        try {
            // ==================== 2. 派活：main 循环 submit，不等待子线程跑完 ====================

            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                // Lambda 内只能用 final 变量，故拷贝 i
                final int index = i;

                /**
                 * 提交任务到线程池；大括号内代码稍后在某条 pool 子线程上执行。
                 * main 不会执行 await / createOrder 这几行。
                 */
                pool.submit(() -> {
                    try {
                        // ---------- 子线程：统一起跑线 ----------
                        // 阻塞直到 main 调用 startGate.countDown()
                        startGate.await();

                        /**
                         * 调用 V2：内部对 goodsId 加 ReentrantLock 再调 OrderInfoService。
                         * userName=junit-t0 ~ junit-t199，便于区分订单、对账。
                         */
                        Result<?> result = orderConcurrencyFacade.createOrder(VERSION, buildOrderDto("t" + index));

                        // 按统一返回码统计成功/失败（与 V1 统计方式一致）
                        if (result.getCode() == ResultCode.SUCCESS.getCode()) {
                            successCount.incrementAndGet();
                        } else {
                            // 常见失败：库存不足（V2 下约 100 成功、100 失败）
                            failCount.incrementAndGet();
                        }
                    } catch (Exception ex) {
                        // 网络、DB、中断等异常也算失败，避免 doneGate 永远不减
                        failCount.incrementAndGet();
                        log.debug("并发下单异常: {}", ex.getMessage());
                    } finally {
                        // 无论成功失败都必须 countDown，否则 main 在 doneGate.await 一直等到超时
                        doneGate.countDown();
                    }
                });
            }

            // ==================== 3. 发令 + 等待（main 线程） ====================

            // 计数 1→0，唤醒所有在 startGate.await() 上的子线程
            startGate.countDown();

            // main 阻塞，最多等 120 秒，直到 200 个子线程都 doneGate.countDown()
            boolean finished = doneGate.await(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertTrue(finished, "并发任务在超时时间内未全部结束");
        } finally {
            // 关闭线程池，不再接受新任务；等待已提交任务收尾
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        // ==================== 4. 查库快照（main 线程，必须在 200 任务完成后） ====================

        /**
         * 从 DB 读取当前 stock、lockStock、待支付订单数等。
         * 与 HTTP {@code GET /api/order/concurrency/stock/result} 逻辑一致。
         */
        Result<ConcurrencyStockResultVO> snapshotResult = concurrencyTestHelper.queryStockResult(
                ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        assertEquals(ResultCode.SUCCESS.getCode(), snapshotResult.getCode());
        ConcurrencyStockResultVO snapshot = snapshotResult.getData();
        assertNotNull(snapshot);

        // 取出最终指标，便于日志与断言
        int success = successCount.get();
        int fail = failCount.get();
        int lockStock = snapshot.getLockStock() == null ? 0 : snapshot.getLockStock();
        int stock = snapshot.getStock() == null ? 0 : snapshot.getStock();
        // Helper 内：lockStock > stock 或 usable < 0 时为 true
        boolean overLocked = Boolean.TRUE.equals(snapshot.getOverLocked());

        // ==================== 5. 控制台报告（复制到 docs/并发/01-压测数据.md V2 行） ====================

        log.info("========== V2 并发压测结果 ==========");
        log.info("并发线程数: {}", CONCURRENT_THREADS);
        log.info("成功下单数: {}", success);
        log.info("失败次数:   {}", fail);
        log.info("当前库存 stock:     {}", stock);
        log.info("锁定库存 lockStock: {}", lockStock);
        log.info("可用库存 usable:    {}", snapshot.getUsableStock());
        log.info("待支付订单数:       {}", snapshot.getPendingOrderCount());
        log.info("是否疑似超锁:       {}", overLocked);
        log.info("=====================================");

        // ==================== 6. 断言（V2 与 V1 的核心差异） ====================

        /** 每个线程都有明确结果，无「丢失」的请求 */
        assertEquals(CONCURRENT_THREADS, success + fail, "成功+失败应等于并发数");

        /**
         * V2 目标：ReentrantLock 串行后，最多只有 100 个线程能依次通过「有货」校验。
         * 第 101～200 个应得到库存不足。若 success > 100，说明 V2 锁未生效或环境有误。
         */
        assertTrue(success <= ConcurrencyTestConstants.DEFAULT_INITIAL_STOCK,
                "V2 成功数应不超过初始库存 100，实际=" + success);

        /** lockStock 不应超过 stock（与 V1 lockStock=109 对比） */
        assertTrue(lockStock <= stock, "V2 lockStock 应不超过 stock，实际 lockStock=" + lockStock);

        /** 综合指标：不应出现 usable 为负等超锁迹象 */
        assertFalse(overLocked, "V2 不应出现超锁");
    }

    /**
     * 构造与前端/压测接口一致的下单 DTO。
     *
     * @param tag 用户名后缀，如 smoke、t0；最终 userName=junit-{tag}，便于 SQL 清理测试数据
     */
    private OrderInfoDTO buildOrderDto(String tag) {
        OrderInfoDTO dto = new OrderInfoDTO();
        dto.setUserName("junit-" + tag);
        dto.setGoodsId(ConcurrencyTestConstants.DEFAULT_TEST_GOODS_ID);
        dto.setGoodsName("小米17");
        dto.setCostPrice(new BigDecimal("3999"));
        dto.setSalePrice(new BigDecimal("4399"));
        dto.setBuyQty(ConcurrencyTestConstants.DEFAULT_BUY_QTY);
        dto.setRemark("V2并发JUnit");
        return dto;
    }
}
