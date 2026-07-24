# 第 8 批：Elasticsearch 面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 互补：[智搜与ES-面试笔记.md](./智搜与ES-面试笔记.md) · [AI功能复习手册.md](./AI功能复习手册.md) · [LangChain与SpringAI对照-复习手册.md](./LangChain与SpringAI对照-复习手册.md)  
> 本批共 **8 题** · 下一批：微服务架构

---

## 题 1：Elasticsearch 是什么？倒排索引原理

================

## 面试问题：

Elasticsearch 解决什么问题？倒排索引是什么？和关系型数据库 B+ 树索引有什么本质区别？

## 考察点：

- 搜索 vs 事务
- 倒排：词 → 文档
- 能说「PG 是账本，ES 是导购目录」

## 标准答案：

**Elasticsearch**：分布式 **搜索与分析** 引擎，基于 Lucene；擅长 **全文检索、相关度排序、聚合统计**。

**倒排索引（Inverted Index）**：

```text
正排：文档 → 包含哪些词
  商品A「小米17 手机」
  商品B「小米手环」

倒排：词 → 哪些文档
  小米 → [A, B]
  17   → [A]
  手机 → [A]
  手环 → [B]
```

搜「小米」→ 直接取 posting list [A,B]，再 **算 BM25 等相关度分数** 排序。

**B+ 树（数据库）**：擅长 **等值、范围、主键**；`LIKE '%小米%'` **前导模糊** 往往无法有效走索引 → 大量扫行，且无「相关度」概念。

## 通俗理解：

**PostgreSQL** = 仓库 **账本**（库存、价格、订单，要准）。  
**Elasticsearch** = 商场 **导购目录**（按关键词快速找货架号，可以比账本晚更新半拍）。

## 项目结合：

- 商品 **模糊搜**「米」「小米17」→ ES 倒排快
- **下单锁库存** → 只走 PostgreSQL + V4 条件 UPDATE，**绝不走 ES**
- 本机学习：`docker run es-dev`，`GET http://127.0.0.1:9200` 验证 8.11.3

## 面试官追问：

1. ES 能替代 MySQL 吗？
2. 什么是 segment？
3. 近实时 NRT 是什么意思？

## 高级回答：

- **不能替代**：无 ACID 事务语义，不适合库存账务。
- **Segment**：Lucene 不可变小索引段；写入先内存 buffer → refresh 可见 → merge 合并。
- **NRT**：默认约 1s refresh，写入后 **近实时** 可搜，非立即。
- 10 年答法：**「交易走库，搜索走 ES；我项目口述用导购目录 vs 账本」**。

================

---

## 题 2：智搜三分工 — V1 / ES / V2（pgvector）

================

## 面试问题：

你们 AI 智搜、Elasticsearch、pgvector 各干什么？为什么不是只用一个？

## 考察点：

- 意图解析 vs 关键词 vs 语义向量
- 不混并发锁库存叙事
- 当前落地状态诚实

## 标准答案：

| 能力 | 做什么 | 技术 | 状态 |
|------|--------|------|------|
| **智搜 V1** | 自然语言 → **结构化筛选 JSON**（品牌、价格区间） | Spring AI + PG 条件查询 | ✅ 已有 |
| **Elasticsearch** | **关键词/分词** 相关性检索 | 倒排索引 | ✅ 接入/演示 |
| **智搜 V2** | **语义相似** TopK + RAG 生成推荐理由 | pgvector + Embedding | ✅ 已有 |

```text
用户：「两千以内适合送长辈的手表」
  V1 → LLM 解析 → { priceMax:2000, category:手表, ... } → SQL
  ES → 分词 match「手表」「长辈」→ goodsId 列表
  V2 → embed 整句 → pgvector <=> TopK → RAG 总结
```

**为什么不只用一个**：

- V1 **懂条件**，但不擅长模糊词匹配
- ES **快搜关键词**，不懂「送长辈」语义
- V2 **懂语义**，成本高、要向量维护；数字库存仍以 **PG 列表接口** 为准

## 通俗理解：

- V1 = **翻译官**（把人话翻译成筛选条件）
- ES = **电话簿按姓检索**（字面对字）
- V2 = **按意思找类似商品**（向量距离近）

## 项目结合：

- 前端商品页：**条件智搜（V1）** + **语义搜索（V2）** + 可选 **ES 关键词**
- `POST /api/ai/product/parse`（V1）；`GET /api/ai/goods/semantic-search`（V2）
- ES 索引 `goods_search`：goodsId、productName、brandName、shelfStatus
- **RAG 不编造库存数字** — 见 AI 手册边界

## 面试官追问：

1. ES 和 pgvector 能合并吗？
2. 为什么 V2 不用 ES dense_vector？
3. LLM 直接搜商品行不行？

## 高级回答：

- **ES 8+ dense_vector** 可做 kNN，但运维与 **混合检索**  tuning 复杂；项目 **PG 同库** 事务简单。
- **纯 LLM**：幻觉、贵、慢；应 **检索 + 生成（RAG）**。
- 10 年答法：**「三件套分工清晰；面试画一条用户 query 分三条箭头」** — 见 [智搜与ES-面试笔记.md](./智搜与ES-面试笔记.md) §0。

================

---

## 题 3：Mapping 与分析器（中文分词）

================

## 面试问题：

什么是 mapping？text 和 keyword 区别？中文搜索为什么要 IK 分词？

## 考察点：

- 字段类型影响查询方式
- analyzer 构建索引 vs search analyzer
- 学习版 vs 生产

## 标准答案：

**Mapping**：索引的 **schema**，定义字段类型、是否分词、是否索引。

| 类型 | 分词 | 用途 |
|------|------|------|
| **text** | 是 | 全文检索 productName |
| **keyword** | 否 | 精确过滤 shelfStatus、skuCode、聚合 |
| **long/date** | — | 范围筛选 |

**Analyzer 流程**：Character Filters → **Tokenizer**（切词）→ Token Filters（小写、停用词）

**中文**：默认 standard 对中文 **按字/ poorly 切**；**IK**（ik_max_word 索引、ik_smart 搜索）更符合「小米17」→ [小米, 17] 等。

**multi-field** 常见：

```json
"productName": {
  "type": "text",
  "analyzer": "ik_max_word",
  "fields": {
    "keyword": { "type": "keyword" }
  }
}
```

## 通俗理解：

mapping 像 **给目录卡规定格式**：书名要拆词查（text），ISBN 要整串精确匹配（keyword）。

## 项目结合：

学习版 `goods_search` mapping：

| 字段 | 类型 | 说明 |
|------|------|------|
| goodsId | long | 回 PG 用 |
| productName | text | 全文 |
| brandName / categoryName | text 或 keyword | 过滤+搜 |
| shelfStatus | keyword | term 精确 |

配置：`spring.elasticsearch.uris`，`app.elasticsearch.enabled=true`（dev）；生产可 `false` 不装配演示 Bean。

## 面试官追问：

1. 修改 mapping 能改已有字段类型吗？
2. 什么是 dynamic mapping 风险？
3. copy_to 干什么？

## 高级回答：

- **已有字段类型不可改**（需 reindex 新索引 + alias 切换）。
- **dynamic**：自动推断可能把数字 id 变 text → 生产常 **strict** 或模板管控。
- **copy_to**：多字段合成 `all_text` 统一搜。
- 10 年答法：**「商品名 text+IK；状态 keyword term；改 mapping 要 reindex」**。

================

---

## 题 4：Query DSL 与 bool 查询

================

## 面试问题：

match 和 term 区别？bool 查询 must/should/filter 怎么用？如何实现「名称含小米且已上架」？

## 考察点：

- 全文 vs 精确
- filter 不计分、可 cache
- 结合商品搜索 API 设计

## 标准答案：

| 查询 | 行为 |
|------|------|
| **term** | 精确匹配 keyword，**不分词** |
| **terms** | 多值 IN |
| **match** | 对 text **分词后** 匹配，算相关度 |
| **match_phrase** | 短语顺序匹配 |
| **range** | 数值/日期范围 |
| **wildcard** | 通配，性能差慎用 |

**bool 组合**：

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "productName": "小米17" } }
      ],
      "filter": [
        { "term": { "shelfStatus": "ON" } }
      ],
      "should": [
        { "match": { "brandName": "小米" } }
      ],
      "minimum_should_match": 0
    }
  }
}
```

- **must**：必须满足，参与算分
- **filter**：必须满足，**不算分**，可缓存
- **should**：加分项，可选

## 通俗理解：

must = **硬性条件**（必须上架）；match = **像不像**（名称相关度）；filter = **硬性但不比谁先谁后**（快）。

## 项目结合：

```java
// Spring Data Elasticsearch / RestClient 示意
BoolQuery.Builder b = new BoolQuery.Builder();
b.must(m -> m.match(ma -> ma.field("productName").query(keyword)));
b.filter(f -> f.term(t -> t.field("shelfStatus").value("ON")));
SearchResponse<GoodsDoc> resp = client.search(s -> s
    .index("goods_search")
    .query(q -> q.bool(b.build()))
    .from(0).size(20), GoodsDoc.class);
```

- V1 LLM 解析出 `brand=小米` → 可转 **filter term**
- 用户纯输入关键词 → **match productName**
- 搜到 goodsId 列表后 **可选回 PG** 查实时库存价

## 面试官追问：

1. match 和 match_phrase 何时用？
2. 相关度 score 怎么算（简述）？
3. from+size 深分页问题？

## 高级回答：

- **BM25**：词频、逆文档频率、字段长度归一；面试说「基于 TF-IDF 改进」即可。
- **深分页**：from 过大要排序丢弃，用 **search_after** 或 **scroll**（导出）。
- 10 年答法：**「filter 上架状态；must match 名称；拿 id 回库补库存」**。

================

---

## 题 5：数据同步 — PG 到 ES

================

## 面试问题：

ES 数据从哪来？如何保证和 PostgreSQL 一致？全量、增量、Canal 各有什么优缺点？

## 考察点：

- 最终一致
- reindex vs MQ vs Canal
- 幂等写入（_id = goodsId）

## 标准答案：

**原则**：PG **写成功为准**；ES 是 **检索副本**，接受 **短暂延迟**。

| 方案 | 做法 | 优点 | 缺点 |
|------|------|------|------|
| **手动/定时全量 reindex** | 扫 PG → bulk index | 简单、可对账 | 实时差、数据量大时重 |
| **MQ 增量** | 商品 CRUD 后发消息 → 消费者 index/update/delete | 较实时、业务清晰 | 要改代码、重复消费要幂等 |
| **Canal 等 CDC** | 订阅 binlog/WAL → 写 ES | 业务无侵入 | 运维复杂、PG 要 logical decoding |

**写入幂等**：文档 `_id = goodsId`，同 id 再 index **覆盖**。

**bulk API**：批量写入，注意 **批次大小** 与 **失败重试**。

## 通俗理解：

账本改价后，**导购目录**可以晚半分钟改——但不能反过来只改目录不改账本。同步就像 **定期复印或改价后立即传真目录**。

## 项目结合：

- 学习：`POST /api/goods/es/reindex` 或脚本全量（见智搜笔记）
- 生产口述：**「先落库再发 MQ，消费者按 goodsId 更新 ES；定时全量兜底对账」**
- 商品 **下架**：PG 更新 + ES **delete by id**
- **未上 MQ** 时诚实说学习环境用 **手动 reindex**，原理懂即可

```text
运营改名 → API 写 PG OK → MQ goods.updated → consumer ES index 同 id
用户搜索 → 读 ES（可能延迟几百 ms～数秒）
下单     → 只读 PG 库存
```

## 面试官追问：

1. 同步失败怎么补偿？
2. 删除商品 ES 忘了删怎么办？
3. 双写 PG+ES 同时成功怎么保证？

## 高级回答：

- **失败**：MQ 重试 + DLQ + **定时对账**（PG id 集合 vs ES）
- **双写**：不推荐业务层同时写；**以 PG 为准异步同步 ES**
- 10 年答法：**「接受最终一致；对账任务扫 updated_at 增量补 ES」**。

================

---

## 题 6：读路径与库存真相

================

## 面试问题：

用户搜索到商品后下单，完整链路是什么？为什么 ES 里的库存不能信？如何设计「搜 + 买」？

## 考察点：

- 搜索副本 vs 权威数据
- id 回库
- 与并发 V4 衔接

## 标准答案：

**完整读路径**：

```text
1. 用户输入关键词/语义
2. ES 或 pgvector 得到 goodsId 候选 + 展示字段（名称、图）
3. （推荐）goodsId 批量回 PG 查 价格、usable_qty、shelfStatus
4. 展示列表
5. 用户下单 → order-service → inventory V4 原子锁库存（与 ES 无关）
```

**为什么不能信 ES 库存**：

- ES **无行级事务**，与 PG **异步同步**
- 搜索侧可能 **缓存旧库存** → 超卖若信 ES 会灾难
- **面试金句**：**搜到 id 再回主库；锁库存永远 PG**

## 通俗理解：

导购目录写「大概还有货」可以；**收银台扣库存必须查账本**。你不能因为目录上写着有货就不查系统直接卖。

## 项目结合：

- 智搜 V2 RAG：**只生成推荐理由**，库存数字走 **列表 API / PG**
- ES search 返回 goodsId → `GET /api/goods/batch?ids=` 补全
- 下单 **InsufficientStock** 来自 inventory **实时 SQL**，不是 ES
- 前端：搜索结果显示「库存以详情页为准」

## 面试官追问：

1. ES 里冗余库存字段有没有场景？
2. 缓存库存和 ES 库存区别？
3. 秒杀搜索列表怎么保证体验？

## 高级回答：

- **冗余库存到 ES**：仅 **展示参考**，下单 **必须再校验**；或干脆 ES **不存库存**。
- **Redis 库存预扣**：秒杀层；ES 不管。
- 10 年答法：**「搜索负责找货，交易负责真库存；我项目 V4 是下单唯一真相」** — 与并发批衔接。

================

---

## 题 7：集群、分片与高可用（概念）

================

## 面试问题：

ES 集群核心概念：index、shard、replica？为什么需要副本？单节点学习版和生产的差距？

## 考察点：

- 水平扩展思路
- 不是要求运维过大规模集群
- 资源（内存）意识

## 标准答案：

**概念**：

| 术语 | 含义 |
|------|------|
| **Index** | 逻辑索引，如 `goods_search` |
| **Shard 分片** | 索引数据水平切分，默认 1 primary |
| **Replica 副本** | 分片拷贝，**高可用 + 读扩展** |
| **Node** | 集群节点 |

**为什么副本**：

- primary 挂了，replica **提升为 primary**
- 查询可打 replica **分担读**

**分片数**：创建 index 后 primary 数 **难改**（需 reindex）；学习单节点 **1 shard 0 replica** 即可。

**资源**：ES **吃堆内存**；`ES_JAVA_OPTS=-Xms512m -Xmx512m` 学习；与 **全家桶微服务同机** 要控内存，必要时 **按需启停**。

## 通俗理解：

分片像 **把导购目录拆成多卷**（A-M、N-Z）；副本像 **每卷复印一份**，丢一本还有备份，多人查时可看不同复印本。

## 项目结合：

- 本机 **single-node** `discovery.type=single-node`，xpack.security 学习可关
- 门店数据量 **百万商品内** 单节点 ES 够；面试说 **知道何时加节点**
- 与 **PostgreSQL 同机宝塔**：避免 ES + 5 个 Java 进程 **OOM 抢内存**
- 生产：**3 节点**、主分片+副本、专用 master eligible 节点（了解即可）

## 面试官追问：

1. 脑裂是什么？
2. 为什么写入要 routing 到 primary shard？
3. ES 和 OpenSearch 关系？

## 高级回答：

- **脑裂**：网络分区两个 master；靠 **minimum_master_nodes**（7.x 前）/ **集群状态** 机制缓解。
- **写入 primary**：保证版本一致；replica 同步 primary。
- **OpenSearch**：AWS 分支，API  largely 兼容 ES 7.x。
- 10 年答法：**「我跑过单节点 Docker；能讲 shard/replica 目的即可，不吹管过百节点」**。

================

---

## 题 8：聚合、监控与 ELK（扩展）

================

## 面试问题：

ES 聚合能做什么？和业务搜索什么关系？ELK 是什么？你们项目有没有用 ES 做日志？

## 考察点：

- terms 聚合 vs search
- 日志与业务索引分离
- 务实：进销存以商品搜索为主

## 标准答案：

**聚合（Aggregations）**：

- **Bucket**：terms 按品牌/分类分组计数
- **Metric**：avg、sum、max
- 例：搜「手机」同时看 **各品牌命中数量**

```json
{
  "size": 0,
  "query": { "match": { "productName": "手机" } },
  "aggs": {
    "by_brand": {
      "terms": { "field": "brandName.keyword", "size": 10 }
    }
  }
}
```

**ELK / Elastic Stack**：

- **E**lasticsearch 存储
- **L**ogstash / Beats 采集
- **K**ibana 展示

用于 **日志、APM、监控**，与 **商品 index** 应 **索引隔离**（不同 index 或集群）。

## 通俗理解：

搜索是 **找商品**；聚合是 **顺道统计：搜到的里小米几个、华为几个**。ELK 像 **把全公司日志倒进同一个搜索引擎里查错**。

## 项目结合：

- **商品侧**：terms 聚合可做 **筛选项动态品牌列表**（进阶）
- **运维**：`monitor:redis:trend` 在 Redis；ES 日志栈 **个人项目可未上**，面试说 **「知道 ELK 查日志，业务 ES 做 goods_search」**
- **AI 操作日志**：仍 PG 表，不用 ES（量小）
- 若上 ELK：**json 日志** + traceId 与 Gateway 串联

## 面试官追问：

1. agg 的 doc_count_error_upper_bound 是什么？
2. 为什么聚合字段要 keyword？
3. ES 写入性能如何优化？

## 高级回答：

- **text 字段不能 agg**（除非 fielddata，生产禁用）；用 **keyword 子字段**。
- **写入优化**：bulk、适当 refresh_interval（如 30s）、副本调 0 再 reindex 后恢复。
- 10 年答法：**「业务搜和日志分 index；进销存演示商品检索；ELK 作为扩展知识」**。

---

## 本批小结

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| 倒排索引 | 词→文档，PG B+树擅长等值 |
| 智搜分工 | V1 条件 / ES 关键词 / V2 向量 |
| mapping | text vs keyword，IK 中文 |
| bool 查询 | match + filter 上架 |
| 同步 | PG 为准，MQ/全量 reindex |
| 读路径 | 搜 id 回库，锁库存 PG |
| 集群 | shard/replica，单节点学习 |
| 聚合/ELK | terms 统计，与业务索引分离 |

下一批：**微服务架构** → [面试题-09-微服务架构.md](./面试题-09-微服务架构.md)
