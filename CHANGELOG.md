# 更新日志（微服务学习仓）

版本独立于单体 `v2.0.0`。Git 标签与父 POM 版本对齐（如 `v3.6.0` / `3.6.0`）。

## [3.6.0] — 2026-07-23 · 智搜 V2 RAG + 业务入口收口

- **智搜 V2**：`ai-service` Embedding（`text-embedding-v3` / 1024）+ pgvector TopK；`searchWithRag` 用命中原文生成推荐说明
- 建表 SQL：`docs/sql/v2_goods_search_embedding.sql`；接口 `POST /ai/goods/reindex-embedding`、`GET /ai/goods/semantic-search`
- **业务回填**：`GET /goods/product/listByIds`（保序 + 列表 VO）；ES reindex 正式路径 `/es/goods/reindex` + 关闭时占位提示
- **本机联调**：各服务 `application-dev.yml` 强制 Nacos 注册 `127.0.0.1`；网关 AI 超时 120s
- 文档：AI 最小学习路线、智搜与 ES 笔记、路线图勾选更新

## [3.5.0] — 2026-07-21 · 并发 V7 幂等 + 补偿

- **V7**：`OrderCreateConcurrencyV7` + `OrderIdempotentService`（Redis SET NX → `PROCESSING` / `DONE:{orderNo}`）；建单叠 V4
- 补偿：`OrderCompensateRegistry` + `OrderConcurrencyCompensateJob`（dev 定时 unlock）；V6 消费者可带 `idempotentKey` 走同一套幂等
- 压测 `?version=v7`：Apifox 同 key 连点只锁 1 次；**JMeter** 200 同 key 平均 RT **62ms**、吞吐 **187.3/s**、异常 0%，`lockStock=1`
- 文档：`并发/步骤-V7.md`、`并发/问答-V7.md`；`并发/01-压测数据.md` 补正式结果
- 注意：`stock/reset` 不清 Redis 幂等 key

## [3.4.0] — 2026-07-20 · 并发 V6 RabbitMQ

- **V6**：`OrderCreateConcurrencyV6` 发 MQ 秒回「已受理」；`OrderLockStockConsumer` 异步走 V4 原子锁库存
- order-service 接入 `spring-boot-starter-amqp`；队列 `order.concurrency.lock.stock`（`RabbitMqConfig`）
- 压测 `?version=v6`：**JMeter** 200 并发 HTTP 全受理，平均 RT **93ms**、吞吐 **258.4/s**、异常 0%；最终 lockStock=100、`overLocked=false`（最终一致下 lockStock 会爬升）
- 文档：`并发/步骤-V6.md`、`并发/问答-V6.md`、`并发/00-总览-V1到V7.md`；演进表以 JMeter GUI 结果为准

## [3.3.0] — 2026-07-19 · 并发 V5 / V5r

- **V5**：`OrderCreateConcurrencyV5` + `GoodsStockRedisLock`（Redis `SET NX EX` + Lua 安全解锁）
- **V5r**：`OrderCreateConcurrencyV5Redisson` + Redisson 看门狗锁（对照压测）
- order-service 接入 Redis / Redisson；压测路径 `?version=v5` / `v5r`
- 文档：`并发/01-压测数据.md` 补 JMeter 结果；新增 `并发/步骤-V5.md`、`并发/问答-V5.md`
- JMeter 200 并发：`overLocked=false`；平均 RT 约 23～25s、吞吐约 6.4/s（与 V2 同量级，慢于 V4）

## [3.2.0] — 同机生产切流

- dev/prd 配置拆分、同机部署脚本、域名 Nginx 切到微服务前端 + Gateway

## [0.1.0-SNAPSHOT] — P0 骨架

- Maven 多模块：`inventory-common` / `inventory-service`(8082) / `order-service`(8083)
- 公共 `Result`；两服务探活 `/ping`
- 未接入 DB / Nacos / Feign / Security
