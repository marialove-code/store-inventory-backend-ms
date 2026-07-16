# Sentinel 最小限流 · 五步学习 + 实操问题

> 范围：仅 `order-service` 的 `/api/order/ping`。  
> 学习框架：介绍 → 有什么用 → 怎么用 → 有什么问题 → 怎么解决。

---

## 1. 介绍

Sentinel = 流量防护（限流 / 熔断 / 降级）。本演示只做 **QPS 限流**。

## 2. 有什么用

防止接口被打爆。本例把 `orderPing` 限成 **每秒 2 次**，连点就会失败，用来体会「被保护」。

## 3. 怎么用（本项目已落地）

| 项 | 位置 |
|----|------|
| 依赖 | `spring-cloud-starter-alibaba-sentinel` |
| 规则 | `SentinelFlowRuleConfig`（代码加载，QPS=2） |
| 注解 | `@SentinelResource("orderPing")` + `blockHandler` |
| 接口 | `GET /api/order/ping` |

启动日志应有：`【Sentinel】已加载流控规则 resource=orderPing, QPS=2`

### 实操验证

```powershell
# 快速连打 10 次（1 秒内应出现部分限流）
1..10 | ForEach-Object { Invoke-RestMethod http://localhost:8083/api/order/ping; Start-Sleep -Milliseconds 50 }
```

或浏览器/Apifox 快速连点。  
成功：`code=200`；限流：`触发 Sentinel 限流...`

---

## 4. 有什么问题（建议你亲自踩）

| # | 问题 | 你怎么复现 |
|---|------|------------|
| P1 | 只加依赖、不加规则 → 怎么连点都不限流 | 将 `app.sentinel.order-ping-flow-enabled` 设为 `false` 后**重启** order-service |
| P2 | 阈值太低 → 正常手速也被挡 | QPS=2 时慢慢点也容易中 |
| P3 | 规则只在内存 | 若删掉代码规则、只靠 Dashboard 配，**重启服务规则没了** |
| P4 | `@SentinelResource` 同类 `this.xxx()` 不生效 | 若抽私有方法自调用，限流可能无效（本演示 Controller 由 Spring 调用，正常） |
| P5 | blockHandler 签名不对 → 限流时直接 500 | 末参必须是 `BlockException`，返回类型一致 |

## 5. 怎么解决

| 问题 | 解决 |
|------|------|
| P1 | 打开开关 `app.sentinel.order-ping-flow-enabled=true`（或保持默认）并重启 |
| P2 | 调大 `rule.setCount(...)`，或按真实容量评估 |
| P3 | 规则持久化（后续可接 Nacos）；学习阶段用代码加载可接受 |
| P4 | 经 Spring Bean 调用；不要同类内部 this 调带注解方法 |
| P5 | 按官方签名写 blockHandler |

---

## 刻意没做（后续）

- Sentinel Dashboard 控制台（yml 里留了 8858 注释）  
- Feign / Gateway 接入 Sentinel  
- 熔断、热点参数、规则推 Nacos  

先把 P1～P5 走通，再扩展。
