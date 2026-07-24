# 第 4 批：MySQL / PostgreSQL 面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 互补：[数据库设计.md](./数据库设计.md) · [智搜与ES-面试笔记.md](./智搜与ES-面试笔记.md)  
> 本批共 **8 题** · 下一批：Spring 全家桶

---

## 题 1：InnoDB 存储引擎与 MVCC

================

## 面试问题：

MySQL InnoDB 和 MyISAM 区别？什么是 MVCC？Read View 如何解决读一致性问题？

## 考察点：

- 事务、行锁、崩溃恢复
- 快照读 vs 当前读
- PostgreSQL 也有 MVCC，能对比一句加分

## 标准答案：

**InnoDB vs MyISAM：**

| | InnoDB | MyISAM |
|---|--------|--------|
| 事务 | 支持 | 不支持 |
| 锁 | 行锁（附表锁） | 表锁 |
| 外键 | 支持 | 不支持 |
| 崩溃恢复 | redo/undo | 弱 |
| 计数 `count(*)` | 需扫描 | 存行数（快但不准） |

**生产默认 InnoDB**；MyISAM 几乎淘汰。

**MVCC（多版本并发控制）**：

- 每行有 **隐藏列**（DB_TRX_ID、DB_ROLL_PTR 等）链到 undo log 历史版本
- **快照读**（普通 SELECT）：根据 **Read View**（活跃事务列表）决定可见哪个版本 → **无锁一致性读**
- **当前读**（`SELECT ... FOR UPDATE`、`UPDATE`、`DELETE`）：读 **最新已提交** 并加锁

**Read View 规则（简化）**：

- 版本 trx_id < min_active → 可见
- \> max_active → 不可见，沿 undo 找旧版本
- 在中间 → 看是否在 active 列表

**PostgreSQL**：同样 MVCC，旧版本存在 **Heap 元组**，vacuum 回收；无 undo 链式指针但思想一致。

## 通俗理解：

MVCC 像 **库存台账保留历史快照**：查账的人看「下单那一刻的账本复印件」，不用挡住正在改账的会计；改账的人（当前读）必须看 **最新真账本** 并加锁。

## 项目结合：

- **商品列表分页 SELECT**：快照读，不加锁，高并发读友好
- **库存扣减** `UPDATE stock SET usable_qty = usable_qty - ? WHERE id = ? AND usable_qty >= ?`：**当前读 + 行锁**，不能用普通 SELECT 读余额再 UPDATE（会超卖）
- 进销存表引擎 **统一 InnoDB**；MyBatis 默认走 InnoDB
- **PostgreSQL** 跑 AI 向量表 `goods_search_embedding`，业务表也可 PG，MVCC 同样适用

## 面试官追问：

1. 什么是幻读？RR 能解决吗？
2. undo log 和 redo log 区别？
3. 为什么 count(*) 慢？怎么优化？

## 高级回答：

- **幻读**：同一事务两次范围读，行数变多；InnoDB RR + **间隙锁/next-key** 在 **当前读** 时可防；快照读不加锁不保证无幻读（可重复读语义对快照读成立）。
- **redo**：物理页崩溃恢复；**undo**：事务回滚 + MVCC 旧版本。
- **count(*)**：近似值用缓存/统计表；精确 count 大表用 **覆盖索引** 或 **汇总表**（看板已售数量）。
- 10 年答法：**「读多写少的报表快照读；扣库存必须当前读+条件更新」**。

================

---

## 题 2：索引原理与最左前缀

================

## 面试问题：

B+ 树索引结构？聚簇索引和非聚簇索引区别？联合索引最左前缀原则？什么情况下索引失效？

## 考察点：

- 为什么用 B+ 树不用 Hash（范围查询）
- 覆盖索引、回表
- 结合进销存查询设计索引

## 标准答案：

**B+ 树**：

- 多路平衡树，**叶子节点存数据/指针且链表相连**，范围 scan 高效
- 非叶子只存 key，树高低，磁盘 IO 少

**MySQL InnoDB**：

- **聚簇索引（主键）**：叶子存 **整行数据**
- **二级索引**：叶子存 **主键值**，查非覆盖列需 **回表**

**最左前缀**：联合索引 `(a, b, c)` 可用于 `a`、`a,b`、`a,b,c` 条件；跳过左列则不能用（如单独 `b` 一般不行，除非 index merge 等优化）。

**常见失效/不走索引**：

- 对列 **函数/隐式类型转换**（`where phone = 13800138000` 若 phone varchar）
- **like '%xx'** 左模糊
- **or** 一侧无索引
- 优化器判断 **全表扫更便宜**（小表、选择性差）
- **不等于**、**not in** 大范围（视情况）

**覆盖索引**：SELECT 列全在索引里，无需回表，如 `(category_id, status, id)` 只查这三列。

## 通俗理解：

索引像 **字典按拼音排序的侧边索引**：先找拼音首字母（最左前缀），再定位正文页码（回表）。  
聚簇索引像 **百科全书按主键号直接把整本书页订在一起**。

## 项目结合：

```sql
-- 商品分页：按分类+状态+时间
INDEX idx_goods_cat_status_time (category_id, status, create_time);

-- 订单：用户查自己的单
INDEX idx_order_user_time (user_id, create_time DESC);

-- 库存扣减：主键或 (goods_id, warehouse_id) 唯一
UNIQUE uk_stock_goods_wh (goods_id, warehouse_id);
```

- **智搜 V1** 解析 JSON 后查 PG：`name like` + `category_id =` → 分类 ID 走索引，name 模糊可能全扫，大表靠 **ES/向量**
- **操作日志** 按 `user_id + create_time` 查，避免单列 time 索引全表扫
- **pgvector**：`embedding vector(1536)` + **HNSW/IVFFlat 索引**，语义检索走 `<=>` 算子，与普通 B-tree 互补

## 面试官追问：

1. 一个表几个索引合适？
2. 为什么要避免宽索引？
3. EXPLAIN 看哪些列？

## 高级回答：

- **EXPLAIN**：type（ref/range/index/all）、key、rows、Extra（Using index、Using filesort、Using temporary）
- **filesort/temporary**：ORDER BY / GROUP BY 无合适索引，大数据量要优化
- 索引 **不是越多越好**：写放大、缓冲池占用、优化器选择困难
- 10 年答法：**「先抓慢 SQL，explain 看重 type 和 rows；进销存分页必带 selective 条件列进联合索引左列」**。

================

---

## 题 3：事务 ACID 与隔离级别

================

## 面试问题：

事务 ACID 含义？MySQL 四种隔离级别分别解决什么问题？你们扣库存用什么隔离级别？

## 考察点：

- 脏读、不可重复读、幻读
- 默认 RR 与业务选择
- 分布式下本地事务边界

## 标准答案：

**ACID**：

| | 含义 |
|---|------|
| Atomicity | 全成功或全回滚（undo） |
| Consistency | 约束不被破坏（库存不为负） |
| Isolation | 并发互不干扰（程度看级别） |
| Durability | 提交后持久（redo） |

**隔离级别**：

| 级别 | 脏读 | 不可重复读 | 幻读 |
|------|------|------------|------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 |
| READ COMMITTED | 否 | 可能 | 可能 |
| REPEATABLE READ | 否 | 否 | 快照读基本否* |
| SERIALIZABLE | 否 | 否 | 否 |

\* InnoDB RR 对 **当前读** 用 next-key 防幻读。

**MySQL 默认 RR**；Oracle/PG 默认 often **RC**。

**扣库存实践**：

- 单库单事务：`@Transactional` + `UPDATE ... WHERE usable_qty >= ?`
- 隔离级别 **RR 默认即可**；关键在 **SQL 原子条件** 而非调 RC/RR
- **跨服务**（order 调 inventory）：本地事务各自提交，需 **最终一致/补偿/TCC/Seata**（项目可口述未全落地）

## 通俗理解：

隔离级别像 **你在改库存时，别人能不能在你旁边同时改同一条记录**。级别越高，并发越低但越安全；扣库存靠 **一条 SQL 原子扣减** 比单纯调高隔离更靠谱。

## 项目结合：

- **下单**：order 插订单 + 调 inventory Feign 扣减 = **两个本地事务**；失败要 **取消订单/补偿库存**（状态机 + 定时对账）
- **商品改价 + 写操作日志**：同库可同一 `@Transactional`，日志与业务一致
- **AI 写 embedding 索引**：`reindexFromDb` 批量提交，失败 rollback 批次
- 避免 **长事务**：报表大查询不要 `@Transactional` 包全程

## 面试官追问：

1. `@Transactional` 同类自调用为什么失效？
2. 只读事务有用吗？
3. 分布式事务你们怎么做的？

## 高级回答：

- **自调用**：绕过代理，事务不生效 → 拆 Service 或 `AopContext.currentProxy()`。
- **readOnly=true**：MySQL 可优化只读路由；PG 明确只读；大查询可用。
- **分布式**：诚实说 **Seata AT 未生产落地**；用 **本地消息表/MQ 最终一致/幂等+对账** 更常见；进销存可讲 **订单状态 + 库存流水** 对账。
- 10 年答法：**「能单库事务就不分布式；跨服务靠业务幂等与状态机，不迷信 2PC」**。

================

---

## 题 4：慢 SQL 排查与 EXPLAIN

================

## 面试问题：

线上接口变慢，如何确认是 SQL 问题？EXPLAIN 关键字段怎么读？如何优化一条慢查询？

## 考察点：

- 监控 → 慢日志 → explain → 改 SQL/加索引
- 避免 select *
- 分页深翻页

## 标准答案：

**排查路径**：

1. APM/日志：接口 RT 变长，trace 到 Mapper 方法
2. **慢查询日志** `slow_query_log`，`long_query_time`
3. `EXPLAIN` / `EXPLAIN ANALYZE`（PG）
4. `show profile` / Performance Schema（可选）
5. 业务：是否 **数据量暴涨**、**缓存失效**、**锁等待**

**EXPLAIN 重点**：

- **type**：`all` 全表最差，`index` 扫索引全表，`range/ref/eq_ref/const` 较好
- **key**：实际用的索引
- **rows**：估算扫描行数
- **Extra**：`Using filesort`、`Using temporary`、`Using index`

**优化手段**：

- 加/改 **联合索引**（WHERE + ORDER BY 对齐）
- **覆盖索引** 减少回表
- 避免 `SELECT *`，只取列
- **深分页**：`LIMIT 100000, 10` 慢 → **延迟关联** 或 **上次最大 id** 游标分页
- 批量改 **小批次**，避免长锁

## 通俗理解：

慢 SQL 像 **仓库找货从第一排扫到最后一排**；EXPLAIN 是 **看仓库管理员实际走了哪条通道、扫了多少排货架**。

## 项目结合：

- **订单列表** 管理员分页：`WHERE status=? ORDER BY create_time DESC LIMIT 20` → 索引 `(status, create_time)`
- **库存预警** 低库存：`usable_qty < safety_stock` 选择性低时可能全表，可 **定时任务算预警表** 而非实时全扫
- **操作日志** 深翻：用 `id > lastId LIMIT 20` 替代大 offset
- **MyBatis Plus** 分页插件会发 count + list，count 慢要 **单独优化 count SQL**

## 面试官追问：

1. 前缀索引是什么？有什么坑？
2. 为什么 OR 难优化？
3. PG 的 EXPLAIN ANALYZE 和 MySQL 区别？

## 高级回答：

- **前缀索引**：长字符串只索引前 N 字符，省空间但 **选择性** 要测，无法覆盖 ORDER BY 全列。
- **OR**：常拆成 UNION ALL 各走索引再合并（优化器有时自动 index merge）。
- **PG EXPLAIN ANALYZE**：**真实执行** 拿 timing，比 MySQL 估算 rows 更准；向量检索看 **Index Scan using hnsw**。
- 10 年答法：**「先 explain 再改，拒绝凭感觉加索引；进销存列表类 SQL 在开发环境用 explain 过一遍」**。

================

---

## 题 5：行锁、死锁与库存超卖

================

## 面试问题：

InnoDB 行锁怎么触发的？什么是间隙锁？死锁怎么产生、怎么排查？如何防止库存超卖？

## 考察点：

- 当前读加锁
- 死锁日志、业务重试
- 乐观锁 vs 悲观锁（库存经典题）

## 标准答案：

**行锁触发**：`UPDATE/DELETE/SELECT ... FOR UPDATE/LOCK IN SHARE MODE` 对 **命中索引** 的行加锁；无索引可能 **锁升级** 到表或锁大量行。

**间隙锁（Gap Lock）**：RR 下，锁定 **索引记录之间的间隙**，防幻插入；next-key = 行锁 + 间隙锁。

**死锁**：两事务 **互相等待** 对方持有的锁；InnoDB **检测后回滚代价小的一端**，应用捕获 **1213 Deadlock** 可重试。

**防超卖（核心）**：

```sql
-- 悲观：单条 UPDATE 原子扣减（推荐）
UPDATE stock
SET usable_qty = usable_qty - #{qty},
    version = version + 1
WHERE goods_id = #{goodsId}
  AND usable_qty >= #{qty};

-- 判断 affectedRows == 1，否则库存不足
```

```sql
-- 乐观：版本号
UPDATE stock SET usable_qty = usable_qty - ?, version = version + 1
WHERE goods_id = ? AND version = ? AND usable_qty >= ?;
-- affectedRows=0 → 重试或失败
```

- **不要** SELECT 读 qty 再在 Java 里 if 判断后 UPDATE（并发必超卖）
- 高并发热点 SKU：**Redis 预扣 + DB 异步** 或 **队列串行化**（并发批 V4/V6 演进）

## 通俗理解：

行锁像 **单件商品只能一个人同时改库存表**；间隙锁像 **货架空位之间不许插队塞新行**。  
超卖像 **两个人同时看到「剩 1 件」都去改，不加原子条件就变 -1**。

## 项目结合：

- **inventory-service** 扣减接口：一条 UPDATE + 影响行数，返回 409
- **order-service** 失败不回滚 inventory 若已扣 → 需要 **Saga/补偿** 或 **先扣库存再建单**
- 项目 **并发 V4 原子锁 / Redis** 是 JVM 层补充，DB 层仍要 **条件 UPDATE**
- 死锁：订单批量改同一 goods_id 顺序 **按 goods_id 排序** 拿锁，减少交叉等待

## 面试官追问：

1. 乐观锁重试三次还失败怎么办？
2. SELECT FOR UPDATE 和 UPDATE 直接扣哪个好？
3. Redis 扣减和 DB 不一致怎么对账？

## 高级回答：

- **乐观锁失败**：返回「系统繁忙请重试」或转 **悲观队列**；热点商品直接 Redis Lua 原子减。
- **FOR UPDATE**：多一步锁 + 再 update，RT 更长；**单 UPDATE 条件扣** 更简洁。
- **对账**：库存流水表 + 定时比对 Redis 与 DB；MQ 削峰后 **最终一致**。
- 10 年答法：**「超卖根因是读-改-写非原子；我项目用 UPDATE where qty>= 兜底，高并发再加 Redis/MQ」** — 与并发批「公共厕所」题衔接。

================

---

## 题 6：主键设计与分库分表入门

================

## 面试问题：

自增 ID 有什么问题？雪花算法原理？什么场景要分库分表？进销存需要吗？

## 考察点：

- 趋势递增对 B+ 树插入的影响（页分裂仍可控）
- 分布式 ID、业务号
- 务实：中小项目不必分库分表

## 标准答案：

**自增 ID**：

- 优点：简单、有序、插入局部性好
- 缺点：**分库分表全局唯一难**、**暴露业务量**、**迁移合并麻烦**

**雪花 Snowflake**：41 时间 + 10 机器 + 12 序列 → 趋势递增 Long，分布式唯一；依赖 **时钟回拨** 处理。

**UUID**：随机，插入 **页分裂多**，做主键性能差，可做 **业务对外单号**。

**分库分表时机**（经验）：

- 单表 **千万～亿级** 且优化到头
- 写 QPS **持续** 超单库能力
- 不是 **CRUD 小项目默认选项**

**分片键**：订单用 **user_id** 或 **order_id**；库存用 **goods_id/warehouse_id**；避免跨片 join。

## 通俗理解：

自增 ID 像 **总部统一发号**，分库后各店自己发会撞号；雪花像 **店号+时间+序号** 拼成全国唯一单号。

## 项目结合：

- **订单号 order_no**：`yyyyMMdd + 序列/雪花` 对外展示，**不用自增 id 给前端**
- **主键 id**：单体 PG/MySQL bigint 自增 **够用**
- **微服务拆分后**：各服务 **独立库**，自增 id **仅库内唯一**，跨服务用 **order_no / goods_id**
- **诚实边界**：门店进销存 **不需要分库分表**；面试说清 **何时才需要** 比吹已分片加分

## 面试官追问：

1. 雪花时钟回拨怎么办？
2. 全局唯一订单号还有哪些方案？
3. 分库后怎么做分页？

## 高级回答：

- **回拨**：等待、借用未来序列、或降级 UUID 段（美团 Leaf 等方案）。
- **订单号**：DB segment（号段）、Redis INCR、雪花均可。
- **跨分片分页**：难，业务改 **按分片键查** 或 **ES 宽表** 检索。
- 10 年答法：**「我项目量级单库 + 合理索引；订单用业务号，为将来分片留分片键字段」**。

================

---

## 题 7：PostgreSQL 与 MySQL 差异及 pgvector

================

## 面试问题：

你们为什么部分用 PostgreSQL？和 MySQL 主要差异？pgvector 怎么做语义检索？

## 考察点：

- 选型务实（AI 向量、JSON、标准 SQL）
- 不是宗教战争
- 向量索引与业务 SQL 共存

## 标准答案：

**常见差异**：

| 维度 | MySQL (InnoDB) | PostgreSQL |
|------|----------------|------------|
| 默认隔离 | RR | RC |
| JSON | JSON 类型 | JSONB 更强（索引 GIN） |
| 扩展 | 插件少 | **扩展丰富**（pgvector、PostGIS） |
| 优化器 | 成熟简单场景 | 复杂查询 often 更强 |
| 复制 | 主从成熟 | 流复制 + 逻辑复制 |

**pgvector**：

- 列类型 `vector(n)` 存 embedding
- 相似度：`<=>`（余弦/ L2 等 ops）
- 索引：**IVFFlat**（快建、近似）、**HNSW**（召回更好，PG 16+ 常用）
- 流程：**离线 embed 写入** → **在线 query embed** → `ORDER BY embedding <=> query LIMIT k`

**与 ES**：

- ES：关键词 + 复杂聚合；向量也可但运维重
- pgvector：**与商品表同库**，事务一致性好做；量级百万内个人项目合适

## 通俗理解：

PostgreSQL 像 **带插件槽的瑞士军刀**；pgvector 是 **在仓库里加「按意思找货」的指南针**，MySQL 原生没有这把刀，要外挂 ES 或单独向量库。

## 项目结合：

- **goods_search_embedding** 表：goods_id、chunk_text、embedding
- **智搜 V2**：`GoodsSemanticSearchService` → embed → SQL TopK → RAG 拼 Prompt
- **智搜 V1** 仍 **PG/MySQL like + 条件**，结构化 JSON 解析
- **业务表** 可仍 MySQL，AI 表 PG — 面试说 **「按能力选型，接受多数据源或逐步迁 PG」**

## 面试官追问：

1. IVFFlat 和 HNSW 怎么选？
2. 向量维度过高有什么问题？
3. 商品更新后向量怎么同步？

## 高级回答：

- **HNSW**：查询快、召回高，占内存；**IVFFlat** 需 lists 训练，大数据建索引快。
- **维度**：存储与计算线性涨；通义 embedding 维度固定，索引按维建。
- **同步**：商品 CRUD 后 **MQ/异步 reindex** 或定时全量；未做实时可诚实说 **手动 reindex 接口**。
- 10 年答法：**「RAG 要检索+生成，检索用 pgvector 够用；关键词仍 ES/like 互补」** — 链到 [智搜与ES-面试笔记.md](./智搜与ES-面试笔记.md)。

================

---

## 题 8：日志、备份与高可用（务实版）

================

## 面试问题：

binlog/redo/undo 各干什么？如何做主从复制？数据库备份策略？进销存单机部署要注意什么？

## 考察点：

- 备份恢复 RPO/RTO 意识
- 不夸大 HA 经验
- 宝塔单机 + 云轻量的真实做法

## 标准答案：

**三种 log**：

| Log | 作用 |
|-----|------|
| **redo** | InnoDB 物理页，崩溃恢复已提交 |
| **undo** | 回滚 + MVCC 旧版本 |
| **binlog** | Server 层逻辑日志，**主从复制、PITR 点时间恢复** |

**主从复制（MySQL）**：从库 IO 线程拉 binlog → relay log → SQL 线程重放；**异步** 默认有延迟。

**PG**：WAL 流复制类似思想。

**备份**：

- **全量** + **binlog/WAL 增量** → 恢复到任意时间点
- 轻量项目：**mysqldump/pg_dump 定时** + 云盘快照
- 恢复 **要演练**，不只备份

**单机宝塔注意**：

- DB 与 Java 同机：抢 **内存与磁盘 IO**
- 定期备份到 **异地**（COS/OSS）
- 连接数：`max_connections` vs Hikari **pool size × 服务数**

## 通俗理解：

redo 像 **写了一半断电后根据草稿恢复**；binlog 像 **录像回放**，可以复制给分店或回到某一刻。

## 项目结合：

- **腾讯云轻量 + 宝塔**：MySQL/PG 单机，**未做主从** — 面试如实说，但知道 **主从读写分离** 原理
- **HikariCP**：每服务 pool 10～20，5 个微服务别打满 DB 连接
- **敏感数据**：备份文件 **加密存储**，不进 Git
- 若 order 容器化而 DB 在宿主机：JDBC URL 用 **宿主机 IP/host.docker.internal**，防火墙只开内网

## 面试官追问：

1. 主从延迟导致读到旧库存怎么办？
2. GTID 是什么？
3. 如何做一次恢复演练？

## 高级回答：

- **读旧库存**：写后读走 **主库**；关键扣减 **只在主库**；缓存 TTL 短 + 失效
- **GTID**：全局事务 ID，failover 不丢 binlog 位点，运维友好
- **演练**：测试环境 restore 备份，跑 smoke 下单
- 10 年答法：**「个人项目全量备份+快照；生产会要求 binlog+PITR 和主从，我懂流程未在大流量管过 DBA」** — 诚实不减分。

================

---

## 本批小结

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| MVCC | 快照读 vs 当前读、扣库存用 UPDATE |
| 索引 | B+ 树、最左前缀、覆盖索引、explain |
| 事务 | ACID、RR 默认、跨服务靠幂等/补偿 |
| 慢 SQL | explain、深分页、避免 select * |
| 锁/超卖 | 条件 UPDATE、死锁重试、乐观/悲观 |
| 主键 | 业务 order_no、不必盲目分库分表 |
| PG/pgvector | 语义检索、与 ES 分工 |
| 备份 HA | binlog/redo 分工、单机备份策略 |

下一批：**Spring 全家桶** → [面试题-05-Spring全家桶.md](./面试题-05-Spring全家桶.md)
