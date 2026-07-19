package com.inventory.modules.order.concurrency.v5;

import com.inventory.common.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 商品维度「手写」Redis 分布式锁 —— 并发 V5 的锁工具类。
 * <p>
 * 【建议阅读顺序】先读本类 tryLock / unlock，再读 {@link OrderCreateConcurrencyV5}。
 * </p>
 * <p>
 * 【一句话原理】<br>
 * 把「锁」做成 Redis 里的一个 Key。谁先用 SET NX 写进去，谁就拿到锁；
 * 别人再 SET NX 会失败，只能等待或超时。业务做完后，用 Lua 校验 token 再 DEL，避免误删别人的锁。
 * </p>
 * <p>
 * 【和 V2 的本质差别】<br>
 * V2 的 ReentrantLock 活在某个 Java 进程的内存里；本类的锁活在 Redis 里，
 * 多个 order-service 实例只要连同一个 Redis，就会抢同一把逻辑锁。
 * </p>
 * <p>
 * 【三个关键点】
 * <ol>
 *   <li>加锁：SET key token NX EX —— NX=不存在才成功，EX=过期秒数（防死锁）</li>
 *   <li>等待：抢不到就 sleep 再试，直到 waitMs 用尽</li>
 *   <li>解锁：Lua「value 仍是我的 token 才 DEL」—— 防误删</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoodsStockRedisLock {

    /**
     * 解锁用的 Lua 脚本（原子执行：先 GET 再决定是否 DEL）。
     * <p>
     * 为啥不用「先 get 再 del 两行 Java」？因为两步之间可能被别人插队，不安全。
     * Redis 执行 Lua 时对该脚本是原子的。
     * </p>
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();

    static {
        UNLOCK_SCRIPT.setResultType(Long.class);
        // KEYS[1] = 锁的 Key；ARGV[1] = 加锁时写入的 token
        // 只有 Redis 里当前值还等于我的 token，才删除；否则返回 0（说明锁已过期或已被别人持有）
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "return redis.call('del', KEYS[1]) "
                        + "else return 0 end"
        );
    }

    /** Spring 封装的 Redis 字符串操作；底层连你们配置的 Redis */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 在 waitMs 毫秒内尝试获取「该商品」的分布式锁。
     * <p>
     * 【成功】返回 token（UUID），后续 unlock 必须带上同一个 token<br>
     * 【失败】返回 null（超时仍未抢到）→ 上层返回「系统繁忙，获取锁超时」
     * </p>
     *
     * @param goodsId      商品 ID，一把锁对应一个商品（不同商品可并行）
     * @param waitMs       最多愿意等多久（压测默认 30000）
     * @param leaseSeconds 锁的租约秒数（压测默认 30）；到期 Redis 自动删 Key，防止持锁进程挂了永远不放
     */
    public String tryLock(Long goodsId, long waitMs, long leaseSeconds) {
        // ---------- 步骤 A：拼出这把锁在 Redis 里的 Key ----------
        // 例：lock:stock:2064625692771397632
        String key = lockKey(goodsId);

        // ---------- 步骤 B：生成本次持锁凭证 token ----------
        // 解锁时要比对「还是不是我」；不能简单 DEL，否则可能删掉别人的锁
        String token = UUID.randomUUID().toString();

        // ---------- 步骤 C：计算截止时间（自旋等待用） ----------
        long deadline = System.currentTimeMillis() + Math.max(waitMs, 0L);
        Duration lease = Duration.ofSeconds(Math.max(leaseSeconds, 1L));

        // ---------- 步骤 D：循环尝试 SET NX，直到成功或超时 ----------
        while (System.currentTimeMillis() <= deadline) {
            // setIfAbsent = Redis：SET key value NX EX
            // 返回 true  → Key 原本不存在，写入成功 → 我拿到锁
            // 返回 false → Key 已存在（别人持锁）→ 继续等
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, token, lease);
            if (Boolean.TRUE.equals(ok)) {
                return token; // 抢锁成功，把 token 交给业务层，finally 里用来解锁
            }
            try {
                // 没抢到：短暂休眠，避免 while 空转把 Redis/CPU 打满
                TimeUnit.MILLISECONDS.sleep(20L);
            } catch (InterruptedException e) {
                // 线程被中断：恢复中断标记并放弃抢锁
                Thread.currentThread().interrupt();
                return null;
            }
        }
        // ---------- 步骤 E：等到截止时间仍未成功 ----------
        return null;
    }

    /**
     * 释放锁：仅当 Redis 中仍是自己的 token 时才删除。
     * <p>
     * 【典型误删场景（本方法要防住）】<br>
     * 1. 我持锁做业务太久，锁 EX 过期被 Redis 删掉<br>
     * 2. 别人抢到了同一 Key<br>
     * 3. 若我此时盲目 DEL，会把别人的锁删掉 → 互斥被破坏<br>
     * 所以必须「校验 token == 我的」再删。
     * </p>
     */
    public void unlock(Long goodsId, String token) {
        if (goodsId == null || token == null || token.isBlank()) {
            return;
        }
        try {
            // 执行上面的 Lua：原子校验 + 删除
            stringRedisTemplate.execute(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(lockKey(goodsId)),
                    token
            );
        } catch (Exception ex) {
            // 解锁失败不把建单结果毁掉：最多等租约 EX 到期自动释放
            log.warn("V5 Redis 解锁异常 goodsId={}：{}", goodsId, ex.getMessage());
        }
    }

    /** 统一 Key 规则，与 RedisConstants 前缀一致，方便在 Redis 客户端观察 */
    private static String lockKey(Long goodsId) {
        return RedisConstants.STOCK_LOCK_PREFIX + goodsId;
    }
}
