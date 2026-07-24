# 第 7 批：Redis 面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 互补：[面试题-RBAC与Redis.md](./面试题-RBAC与Redis.md) · [面试题-06-并发编程.md](./面试题-06-并发编程.md)（V5/V7 锁与幂等）  
> 本批共 **8 题** · 下一批：Elasticsearch

---

## 题 1：Redis 数据类型与选型

================

## 面试问题：

Redis 有哪些基本数据类型？各自典型场景？你们项目里 Redis 存什么、不存什么？

## 考察点：

- String / Hash / List / Set / ZSet 语义
- 缓存 vs 主数据存储边界
- 结合进销存真实 Key 设计

## 标准答案：

| 类型 | 底层直觉 | 典型场景 |
|------|----------|----------|
| **String** | 字节串 | 缓存 JSON、计数、分布式锁、SETNX 幂等 |
| **Hash** | field-value  map | 对象部分字段、多设备 token 映射 |
| **List** | 双向链表 | 消息队列（简单）、最新列表 |
| **Set** | 无序去重 | 标签、共同好友、黑白名单 |
| **ZSet** | score 排序 | 排行榜、延迟队列 |

**Redis 适合**：热点读、会话、计数、锁、限流 — **内存、快、过期**。  
**Redis 不适合**：订单/库存 **权威账本**（应 PostgreSQL/MySQL）；大 value；无过期海量 key。

## 通俗理解：

Redis 像 **收银台旁的极速备忘录**：登录谁在店、今日限流次数、抢坑通行证；**真正库存台账** 仍在仓库数据库里。

## 项目结合：

| 用途 | 类型 | Key 示例（前缀 `inventory:`） |
|------|------|--------------------------------|
| 登录态 | String | `user:token:{userId}:access:{token}` |
| 权限缓存 | String/JSON | `user:perm:{userId}` |
| 多设备 | Hash | `user:device:{userId}` |
| 接口限流 | String 计数 | `limit:{uri}:{ip}` |
| 分布式锁 V5 | String | `lock:goods:{goodsId}` |
| 幂等 V7 | String SETNX | `idempotent:{key}` |

**不存**：商品主数据、订单明细、库存最终数（以 DB 为准）。

## 面试官追问：

1. String 底层 SDS 和 C 字符串区别？
2. Hash 什么时候比多个 String 好？
3. Redis 单线程为什么还快？

## 高级回答：

- **SDS**：O(1) 长度、二进制安全、预分配减少 realloc。
- **Hash**：字段级更新、内存 ziplist/listpack 编码省空间；`user:device` 多 token 映射。
- **快**：内存、IO 多路复用、单线程无锁竞争（6.0+ 多 IO 线程处理网络）。
- 10 年答法：**「Redis 是加速层和协调层，不是进销存 SOR；Key 带前缀、必设 TTL」**。

================

---

## 题 2：缓存穿透、击穿、雪崩

================

## 面试问题：

什么是缓存穿透、击穿、雪崩？分别怎么防？和库存/商品查询怎么结合？

## 考察点：

- 问题定义准确
- 布隆过滤器、空值缓存、互斥锁、TTL 打散
- 不把所有问题都答成「加 Redis」

## 标准答案：

| 问题 | 现象 | 常见方案 |
|------|------|----------|
| **穿透** | 查 **不存在** 的数据，缓存没有，**每次都打 DB** | 布隆过滤器；**空值缓存**短 TTL；接口校验非法 id |
| **击穿** | **热点 key 过期瞬间**，大量请求同时打 DB | 互斥锁重建；逻辑过期（异步刷新）；热点 key 不过期+后台更新 |
| **雪崩** | **大量 key 同时过期** 或 Redis **宕机**，DB 被打垮 | TTL **加随机抖动**；集群高可用；限流降级；多级缓存 |

## 通俗理解：

- **穿透**：有人反复问「店里有外星人商品吗」——库里没有，缓存也没有，每次都去仓库翻一遍。
- **击穿**：爆款商品缓存 **刚过期一秒**，一万个人同时涌向数据库。
- **雪崩**：太多缓存 **同一分钟过期**，或 Redis 整个停电，所有请求砸向数据库。

## 项目结合：

- **商品详情**：`goods:{id}` 缓存 30min；不存在 id 缓存 `null` 5min 防穿透
- **权限 `user:perm:{userId}`**：改角色时 **主动 delete**，不是等过期（击穿风险低，因 key 分散）
- **热点 SKU 库存**：**不以 Redis 为权威**；若做预扣缓存，过期需与 DB 对账
- **TTL**：`30min + random(0,300s)` 打散雪崩

```java
// 空值防穿透（示意）
public GoodsVO getGoods(Long id) {
    String cacheKey = "goods:" + id;
    String json = redis.get(cacheKey);
    if ("NULL".equals(json)) return null;
    if (json != null) return parse(json);
    GoodsVO vo = goodsMapper.selectById(id);
    if (vo == null) {
        redis.setex(cacheKey, 300, "NULL");
    } else {
        redis.setex(cacheKey, 1800 + random(), toJson(vo));
    }
    return vo;
}
```

## 面试官追问：

1. 布隆过滤器能删吗？误判怎么办？
2. 缓存和 DB 双写一致性策略？
3. 先删缓存还是先更新 DB？

## 高级回答：

- **布loom**：一般不支持删；误判 → 穿透到 DB 再确认；商品 id 场景可用。
- **一致性**：**Cache Aside** — 读 miss 加载写缓存；写 DB **后删缓存**（或延时双删）；强一致用 Canal/MQ 异步删。
- **先更新 DB 再删缓存** 较常见；删失败可 MQ 补偿。
- 10 年答法：**「进销存库存以 DB 原子 UPDATE 为准；Redis 缓存商品展示，穿透用空值+非法 id 校验」**。

================

---

## 题 3：JWT + Redis 双校验登录态

================

## 面试问题：

为什么有 JWT 还要 Redis 存 token？登出、踢人、续期怎么做？

## 考察点：

- JWT 无状态优缺点
- 本项目真实链路（RBAC 文档）
- 安全与可运维性

## 标准答案：

**纯 JWT 问题**：

- 签发后 **无法服务端废止**（登出仍有效至 exp）
- 无法 **踢人、单设备登录、权限变更立即生效**（除非极短 exp + refresh）

**本项目：JWT + Redis 双校验**：

1. 登录成功：签发 JWT，**同时** Redis 存 `user:token:{userId}:access:{token}` → `LoginUserVO`
2. 请求：`JwtFilter` 验签 + 查 Redis **存在且未过期**
3. 登出/踢人：**删 Redis key**（JWT 即使未过期也失效）
4. Refresh：`refreshToken` 存 Redis，轮换 access

**权限缓存**：`user:perm:{userId}` 登录写入；后台改权限 **del 该 key**，下次加载新权限。

## 通俗理解：

JWT 像 **带防伪标记的入场腕带**（自包含信息）；Redis 像 **门口实时名单**——名单划掉你，腕带还在也进不去。

## 项目结合：

```
请求 → Gateway/Filter 解析 JWT
     → Redis GET user:token:{uid}:access:{token}
     → 无记录 → 401「登录已失效」
     → 有 → PermissionAspect 查 perm（或 LoginUserVO 内权限）
```

- **多设备**：`user:device:{userId}` Hash 存 access→refresh 映射
- **限流**：`limit:{uri}:{ip}` 与登录态独立
- 面试强调：**不是有 JWT 就一定有效** — 被踢下线后 Redis 无记录即拦

## 面试官追问：

1. JWT 放 Header 还是 Cookie？
2. refresh token 被盗怎么办？
3. 权限变更如何不失效 JWT 内旧权限？

## 高级回答：

- **Bearer Header**：SPA 常用；Cookie 要 HttpOnly 防 XSS 但 CSRF 要防。
- **refresh 被盗**：rotation（每次刷新换新 refresh）、设备绑定、异常 IP 告警。
- **权限变更**：JWT 少放权限或放 version；**以 Redis/DB 实时 perm 为准**（本项目 LoginUserVO + perm 缓存）。
- 10 年答法：**「双校验平衡无状态与可控；登出删 Redis 是标准答法」** — 见 [面试题-RBAC与Redis.md](./面试题-RBAC与Redis.md)。

================

---

## 题 4：分布式锁（V5）与 Redis 实现

================

## 面试问题：

Redis 分布式锁如何实现？有什么问题？Redisson 看门狗？和库存 V4 SQL 如何分工？

## 考察点：

- SET NX EX、Lua 解锁
- 过期、误删、主从切换
- 不替代 DB 原子扣库存

## 标准答案：

**正确加锁**：

```redis
SET lock:goods:1001 <unique-token> NX EX 30
```

**正确解锁（Lua 原子）**：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
  return redis.call('del', KEYS[1])
else return 0 end
```

**常见问题**：

| 问题 | 说明 |
|------|------|
| 锁过期业务未完 | 看门狗续期；或 lease 估足 + 监控 |
| 误删他人锁 |  value 用 UUID，Lua 校验 |
| 非原子 unlock | 必须 Lua |
| 主从切换丢锁 | RedLock 争议；金融级要 ZK/DB |

**与 V4 分工**：

- **锁库存防超卖** → **V4 条件 UPDATE**（权威）
- **Redis 锁** → 跨 JVM **串行临界区**（V5 教学）、**防重复提交辅助**、**定时任务单跑**

## 通俗理解：

Redis 锁是 **全楼共享的临时通行证**（见并发批厕所类比）；**坑位真实数量** 仍以 **数据库电子门锁（V4）** 为准。通行证管「流程串行」，门锁管「绝不能超占」。

## 项目结合：

`GoodsStockRedisLock`：

```java
Boolean ok = stringRedisTemplate.opsForValue()
    .setIfAbsent(key, token, Duration.ofSeconds(lease));
// unlock: execute Lua with token
```

V5r：`GoodsStockRedissonLock` 看门狗自动续期。  
压测：V5 RT ~23s **慢于 V4 ~14s** — 面试说 **生产扣库存不首选 Redis 锁**。

## 面试官追问：

1. SETNX 和 SET NX 区别？
2. 锁可重入 Redis 怎么做？
3. 为什么不要用 Redisson 锁库存替代 SQL？

## 高级回答：

- 老 `SETNX` 无过期需 `EXPIRE` 非原子；**SET key value NX EX** 一条搞定。
- **可重入**：Hash 结构 `HINCRBY lock:threadId count`，Redisson 内置。
- **SQL 更稳**：锁与数据同库，原子 UPDATE 一步；Redis 锁 + 非原子 SQL 只是演示。
- 10 年答法：**「会手写 NX+Lua；生产 inventory 用 lockStockIfAvailable」**。

================

---

## 题 5：幂等与限流

================

## 面试问题：

Redis 如何实现接口幂等和限流？固定窗口、滑动窗口、令牌桶区别？项目里怎么用？

## 考察点：

- SET NX 幂等
- 限流算法
- @RateLimit 实现思路

## 标准答案：

**幂等（V7）**：

```redis
SET idempotent:{clientRequestId} {orderNo} NX EX 86400
```

- 成功 → 继续 V4 占坑
- 失败 → 返回已存在结果
- DB **唯一索引** `client_request_id` 双保险

**限流 — 固定窗口**：

```redis
INCR limit:/api/order/create:192.168.1.1
EXPIRE key 60
-- count > 100 则 429
```

**滑动窗口**：ZSet score=时间戳，删窗口外，count 成员。  
**令牌桶**：Redisson `RRateLimiter` 或 Guava + Redis 协调。

| 算法 | 特点 |
|------|------|
| 固定窗口 | 实现简单，边界 **2 倍突发** |
| 滑动窗口 | 更平滑 |
| 令牌桶 | 允许 **一定突发**，平滑限流 |

## 通俗理解：

**幂等**：同一个 **排队号** 只能领一次坑位号。  
**限流**：同一 IP 每分钟只能 **敲门口 100 次**，防止把接待员砸晕。

## 项目结合：

- **V7**：`idempotentKey` + Redis NX，200 并发同 key → lockStock=1
- **@RateLimit**：Key `limit:{uri}:{ip}`，配合 Sentinel **双层**（网关 + 应用）
- **登录防刷**：`/api/auth/login` 限 IP
- 与 **Sentinel** 区别：Redis 限流 **分布式精确计数**；Sentinel **QPS/线程数/熔断** 更全面

```java
// 固定窗口限流（示意）
Long count = redis.opsForValue().increment(key);
if (count == 1) redis.expire(key, windowSeconds);
if (count > maxRequests) throw new RateLimitException();
```

## 面试官追问：

1. 幂等键过期后用户重复提交怎么办？
2. 限流 Redis 挂了怎么办？
3. 集群下 INCR 精确吗？

## 高级回答：

- **幂等过期**：业务单已落库则 **DB 唯一约束** 兜底；过期时间覆盖「用户可能重试窗口」。
- **Redis 挂**：降级 **本地限流** 或 **fail open**（放行）vs **fail close**（拒绝）看业务；登录可 fail close。
- **INCR**：单 key 原子，集群下 key 在同 slot（hash tag `{ip}`）即可。
- 10 年答法：**「下单幂等键 + 限流保护 auth 和 create；Sentinel 管熔断，Redis 管细粒度计数」**。

================

---

## 题 6：持久化 RDB 与 AOF

================

## 面试问题：

RDB 和 AOF 区别？混合持久化？Redis 挂了登录态和锁会怎样？

## 考察点：

- 持久化非不丢，要配合策略
- 业务可恢复性设计
- 轻量部署务实

## 标准答案：

| | RDB | AOF |
|---|-----|-----|
| 方式 | 快照 dump | 写命令日志 |
| 恢复 | 快 | 慢（rewrite 压缩） |
| 丢失 | 两次快照间 | 取决于 fsync：always/everysec/no |
| 体积 | 紧凑 | 大，AOF rewrite 压缩 |

**Redis 4+ 混合**：RDB 全量 + AOF 增量，重启快且丢得少。

**fsync 策略**：

- `always`：最安全，最慢
- `everysec`：默认，最多丢 1 秒
- `no`：操作系统刷盘，可能丢更多

**登录态/锁**：内存数据；宕机未持久化 → **全员需重新登录**；**锁丢失** → 靠 DB V4 仍不超卖，但可能 **重复业务** → 要幂等。

## 通俗理解：

RDB 像 **定期拍照**；AOF 像 **录像带**。停电后：照片少录几分钟，或录像带断几秒。

## 项目结合：

- **腾讯云轻量单机 Redis**：开 AOF everysec + 云盘备份即可
- **token 丢了**：用户重新登录，可接受
- **库存锁 key 丢了**：V4 SQL 仍正确；**不要用 Redis 当库存真相**
- 生产：**主从 + 哨兵** 或 **云 Redis 高可用**

## 面试官追问：

1. RDB 触发方式 bgsave 会阻塞吗？
2. AOF rewrite 做什么？
3. 为什么 Redis 不适合存订单？

## 高级回答：

- **bgsave**：fork 子进程写盘，主进程继续；**copy-on-write** 内存翻倍风险。
- **rewrite**：合并重复 SET，缩小 AOF。
- **订单**：要持久、事务、复杂查询；Redis 是缓存/协调。
- 10 年答法：**「个人项目 AOF everysec；知道宕机登录态丢；业务靠 DB」**。

================

---

## 题 7：主从、哨兵与集群

================

## 面试问题：

Redis 主从复制原理？哨兵干什么？Cluster 分片解决什么？你们项目需要吗？

## 考察点：

- 读写分离、故障转移
- 槽位 16384
- 务实：门店进销存单机够

## 标准答案：

**主从复制**：

1. 从库 PSYNC → 全量 RDB + 增量 repl_backlog
2. 异步复制 → **主从延迟**，读从可能旧数据
3. 用途：读扩展、备份、高可用基础

**哨兵 Sentinel**：

- 监控 master；主观下线 / 客观下线
- **自动 failover** 提升从为新 master
- 客户端连哨兵拿 **当前 master 地址**

**Cluster**：

- **16384 slot** 分片，多 master
- 数据分散，**水平扩展**；MOVED/ASK 重定向
- 解决 **单机内存上限** 和 **写 QPS 上限**

**进销存个人项目**：单机 Redis **够用**；面试知 **何时上哨兵/集群** 即可。

## 通俗理解：

主从像 **总店记账 + 分店复印账本**（可能慢半拍）；哨兵像 **总店挂了自动换分店当总店**；Cluster 像 **按商品品类分到不同仓库** 省空间。

## 项目结合：

- 当前：**单机 Redis**，JWT、限流、V5 锁、幂等
- 若 **多 Gateway + 多 order** 实例：仍 **共用一个 Redis** 即可（非必须 Cluster）
- **读从库缓存 perm**：延迟导致权限旧 **可接受短窗口** 或 **写主读主**
- 云 **托管 Redis** 自带 HA，比自己搭哨兵省心

## 面试官追问：

1. 主从延迟怎么监控？
2. Cluster 跨 slot 事务？
3. 分布式锁在 Cluster 下注意什么？

## 高级回答：

- **延迟**：`INFO replication` lag；业务敏感读走 master。
- **Cross slot**：Multi key 要 **同 hash tag** `{goods:1001}:lock` `{goods:1001}:stock`。
- **RedLock** 在 Cluster 要多个 master 节点（争议题，知即可）。
- 10 年答法：**「我项目单机；能讲清哨兵 failover 和 Cluster slot 就达中高级」**。

================

---

## 题 8：Spring 集成与生产规范

================

## 面试问题：

Spring Boot 如何集成 Redis？RedisTemplate 和 Lettuce/Jedis 区别？序列化、Key 规范、线上要注意什么？

## 考察点：

- Lettuce 异步、连接池
- JSON 序列化坑
- Key 前缀、TTL、监控

## 标准答案：

**集成**：

```xml
spring-boot-starter-data-redis
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
```

**Lettuce vs Jedis**：Lettuce **Netty 异步**、线程安全，Boot 2+ 默认；Jedis 老项目常见。

**RedisTemplate**：

- `StringRedisTemplate` — String key/value，**锁、计数、token** 首选
- `RedisTemplate<String, Object>` — 需配置 **Jackson2JsonRedisSerializer**，注意 **类信息 @class** 安全与版本升级

**生产规范**：

| 规范 | 说明 |
|------|------|
| Key 前缀 | `inventory:user:token:` 防冲突、便于 scan |
| 必设 TTL | token、perm、限流、幂等 |
| 避免 big key | 大 Hash/List 拆分 |
| 避免 hot key | 本地缓存 + 随机过期 |
| 监控 | 内存、evicted、connected_clients、慢日志 |

## 通俗理解：

Spring 集成像 **给 Java 程序配一个标准 Redis 插座**；序列化选错像 **把中文备忘录译成乱码**，取出来对不上。

## 项目结合：

- 统一前缀 **`inventory:`**（RBAC 文档）
- **StringRedisTemplate** 存 token JSON、锁 token、限流计数
- **权限变更**：`redis.delete("user:perm:" + userId)` 主动失效
- **连接池**：微服务多实例 × pool size **勿超过 Redis maxclients**
- **AI 模块**：若缓存 Prompt 结果，**短 TTL** + Key 含 model 版本

```java
@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

## 面试官追问：

1. 缓存与 Spring Cache `@Cacheable` 关系？
2. Redis 事务能用吗？
3. pipeline 什么场景？

## 高级回答：

- **@Cacheable**：抽象层，底层仍 Redis；本项目部分手写 Template 更可控。
- **Redis 事务**：MULTI/EXEC，**无 rollback**；不如 Lua **原子脚本**。
- **Pipeline**：批量 GET 多个 perm，减少 RTT；非事务。
- 10 年答法：**「Lettuce+StringRedisTemplate；Key 带 inventory 前缀和 TTL；序列化 JSON 慎用在 ObjectTemplate」**。

================

---

## 本批小结

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| 数据类型 | String 锁/计数，Hash 多设备 |
| 三大问题 | 穿透空值、击穿互斥、雪崩抖动 |
| JWT+Redis | 双校验、登出删 key |
| 分布式锁 | NX+Lua，库存靠 V4 不靠锁 |
| 幂等限流 | SETNX、INCR 固定窗口 |
| 持久化 | RDB/AOF，宕机登录态丢 |
| 高可用 | 主从哨兵 Cluster 场景 |
| Spring | Lettuce、前缀、TTL |

下一批：**Elasticsearch** → [面试题-08-Elasticsearch.md](./面试题-08-Elasticsearch.md)
