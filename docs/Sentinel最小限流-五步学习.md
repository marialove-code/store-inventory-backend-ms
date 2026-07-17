# Sentinel 最小限流 / 熔断 · 五步学习 + 实操问题

> 范围：探活参考（ping / unstable）+ **真实业务**（下单 / 取消 + Feign 锁解锁库存）。  
> 学习框架：介绍 → 有什么用 → 怎么用 → 有什么问题 → 怎么解决。  
> **Nacos / Sentinel 启动命令速查：** [`本机启动命令-Nacos-Sentinel.md`](./本机启动命令-Nacos-Sentinel.md)  
> **面试题（流控/熔断/热点等细节）：** [`面试题-Sentinel.md`](./面试题-Sentinel.md)

---

## 1. 介绍

Sentinel = 流量防护（限流 / 熔断 / 降级）。

| 能力 | 一句话 | 本项目演示 |
|------|--------|------------|
| **限流** | 请求太多，拦住多余的 | `orderPing` QPS=2 |
| **熔断** | 下游老失败/太慢，先断开一段时间 | `orderUnstable` 异常比例 ≥50% |

二者都会抛 `BlockException` → 可走 `blockHandler`。

## 2. 有什么用

| 能力 | 本项目怎么用 |
|------|----------------|
| 代码限流（参考） | `orderPing` QPS=2 |
| 代码限流（业务） | `orderCreate` / `orderCancel`（默认各 QPS=20，可配） |
| 代码熔断（参考） | `orderUnstable` 异常比例 |
| 代码熔断（业务） | `inventoryLock` / `inventoryUnlock`（Feign 锁/还库存） |
| Dashboard | 看实时 QPS、在页面改规则（仍属内存，重启注意） |

## 3. 怎么用

### 3.1 限流（已落地）

| 项 | 位置 |
|----|------|
| 依赖 | `spring-cloud-starter-alibaba-sentinel` |
| 规则 | `SentinelFlowRuleConfig`（可用开关关闭） |
| 注解 | `@SentinelResource("orderPing")` + `blockHandler` |
| 接口 | `GET /api/order/ping` |
| 控制台地址 | `spring.cloud.sentinel.transport.dashboard=127.0.0.1:8858` |

### 3.1b 熔断（已落地）

| 项 | 位置 |
|----|------|
| 规则 | `SentinelDegradeRuleConfig`（开关 `app.sentinel.order-unstable-degrade-enabled`） |
| 注解 | `@SentinelResource("orderUnstable")` + `blockHandler` |
| 接口 | `GET /api/order/demo/unstable?fail=true\|false` |

**本机验证（重启 order-service 后）：**

```text
# 1）连续失败 ≥5 次（计入异常比例）
http://127.0.0.1:8083/api/order/demo/unstable?fail=true

# 2）立刻再打成功路径 —— 若已熔断，应走 blockHandler（提示熔断），而不是返回 OK
http://127.0.0.1:8083/api/order/demo/unstable?fail=false

# 3）等约 10 秒后再打 fail=false，应恢复返回 OK（半开探测成功）
```

也可经 Gateway：`http://127.0.0.1:8080/api/order/demo/unstable?fail=true`（需 Gateway + 路由已配）。

### 3.1c 真实业务（已落地，ping 仍保留作参考）

| 资源 | 挂载点 | 规则 |
|------|--------|------|
| `orderCreate` | `OrderInfoController#add` → `POST /api/order/info/add` | 流控 QPS（默认 20） |
| `orderCancel` | `OrderInfoController#cancel` → `PUT /api/order/info/{id}/cancel` | 流控 QPS（默认 20） |
| `inventoryLock` | `InventoryStockClient#lock` | 熔断（异常比例） |
| `inventoryUnlock` | `InventoryStockClient#unlock` | 熔断（异常比例） |
| `inventoryUsable` | `InventoryStockClient#getUsableStock`（下单入口；勿挂同类内部 getUsable，会自调用绕过 AOP） | 熔断（异常比例） |

开关见 `application.yml`：`app.sentinel.order-business-flow-enabled`、`inventory-degrade-enabled`、`order-create-qps` 等。

**验证建议：** 前端经 Gateway 正常下单/取消应不受影响（QPS=20）；想看限流可把 `order-create-qps` 改成 `2` 后重启 order，再连点下单。熔断可停掉 inventory 再连续下单，观察 `inventoryLock` 打开后快速失败。

### 3.2 启动 Dashboard（你本机操作）

完整复制区见 [`本机启动命令-Nacos-Sentinel.md`](./本机启动命令-Nacos-Sentinel.md)。版本与 SCA 对齐：**1.8.6**（勿用 8080，用 **8858**）。

1. 下载 jar 放到 `E:\tools\sentinel\`：  
   https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar  

2. 启动（CMD）：

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.19
cd /d E:\tools\sentinel
java -Dserver.port=8858 -jar sentinel-dashboard-1.8.6.jar
```

3. 浏览器：http://127.0.0.1:8858  
   默认账号密码：`sentinel` / `sentinel`

4. **重启 order-service**（已配 dashboard 地址）

5. **先访问几次** ping（懒加载：没流量时控制台可能看不到应用）：

```text
http://localhost:8083/api/order/ping
```

6. 控制台左侧应出现应用名 **`order-service`**  
   - **簇点链路**：能看到资源 `orderPing`  
   - **流控规则**：可看到/修改 QPS（启动时代码已加载 QPS=2）

### 3.3 在控制台改规则（体验）

1. 流控规则 → 找到 `orderPing` → 编辑，把单机阈值改成 `5` → 保存  
2. 再连点 ping：限流会变「松」一点  
3. **重启 order-service**：又变回代码里的 QPS=2（见问题 P3）

开关对比（无控制台也能做）：

```yaml
app.sentinel.order-ping-flow-enabled: true   # / false
```

---

## 4. 有什么问题

| # | 问题 | 怎么复现 |
|---|------|----------|
| P1 | 没规则 → 不限流 | `order-ping-flow-enabled=false` 后重启 |
| P2 | QPS 太低 → 手速也会被挡 | 保持 QPS=2 连点 |
| P3 | Dashboard 改的规则重启丢失 | 控制台改成 5 → 重启服务 → 又变回 2 |
| P4 | 控制台没有应用 | 没起流量；或 `eager=true` 导致心跳时还没读到 dashboard 地址（日志：`Dashboard server address not configured`） |
| P5 | Dashboard 用 8080 起不来或乱 | 与 Gateway 冲突 → 必须用 **8858** |
| P6 | 版本不对 | Dashboard 与客户端差太多可能异常 → 用 1.8.6 |
| P7 | 开了多个 Dashboard 进程 | 端口混乱，只保留一个 `8858` 进程 |

## 5. 怎么解决

| 问题 | 解决 |
|------|------|
| P1 | 打开开关或控制台新增流控规则 |
| P2 | 调大 count（代码或控制台） |
| P3 | 学习阶段接受「内存规则」；生产再做 Nacos 持久化 |
| P4 | `eager=false` + main 里提前设置 `csp.sentinel.dashboard.server`；**重启 order**；再访问几次 ping；刷新控制台 |
| P5 | `java -Dserver.port=8858 -jar ...` |
| P6 | 下载 1.8.6 的 dashboard jar |
| P7 | 任务管理器结束多余 java，只留一个 Dashboard |

---

## 刻意还没做

- 规则持久化到 Nacos  
- Feign / Gateway 接 Sentinel  
- 慢调用比例熔断（当前只做了异常比例）  

---

## 端口一览（避免混）

| 进程 | 端口 |
|------|------|
| Gateway | 8080 |
| Nacos | 8848 |
| **Sentinel Dashboard** | **8858** |
| order-service | 8083 |
| Sentinel 客户端回调 | 8719（默认） |
