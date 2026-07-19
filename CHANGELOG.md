# 更新日志（微服务学习仓）

版本独立于单体 `v2.0.0`。Git 标签与父 POM 版本对齐（如 `v3.3.0` / `3.3.0`）。

## [3.3.0] — 2026-07-19 · 并发 V5 / V5r

- **V5**：`OrderCreateConcurrencyV5` + `GoodsStockRedisLock`（Redis `SET NX EX` + Lua 安全解锁）
- **V5r**：`OrderCreateConcurrencyV5Redisson` + Redisson 看门狗锁（对照压测）
- order-service 接入 Redis / Redisson；压测路径 `?version=v5` / `v5r`
- 文档：`并发演进.md` 补 JMeter 结果；新增 `并发V5-压测步骤.md`、`并发V5-问答整理.md`
- JMeter 200 并发：`overLocked=false`；平均 RT 约 23～25s、吞吐约 6.4/s（与 V2 同量级，慢于 V4）

## [3.2.0] — 同机生产切流

- dev/prd 配置拆分、同机部署脚本、域名 Nginx 切到微服务前端 + Gateway

## [0.1.0-SNAPSHOT] — P0 骨架

- Maven 多模块：`inventory-common` / `inventory-service`(8082) / `order-service`(8083)
- 公共 `Result`；两服务探活 `/ping`
- 未接入 DB / Nacos / Feign / Security
