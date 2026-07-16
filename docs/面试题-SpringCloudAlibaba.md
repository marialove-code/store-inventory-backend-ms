# 面试题 · Spring Cloud Alibaba（当前进度）

> 覆盖：微服务/集群/负载均衡基础原因 + 本仓库已落地的 Nacos、OpenFeign、LoadBalancer、Gateway。  
> **后续边做边补**（Config / Sentinel / Seata 等）。  
> 实操笔记：[`治理层接入笔记-Nacos-Feign-Gateway.md`](./治理层接入笔记-Nacos-Feign-Gateway.md)

---

## 1. 微服务 / 集群 / 负载均衡（为什么这么做）

### Q1：什么是单体？什么是微服务？

**答：**  

- **单体**：一个进程里放登录、商品、库存、订单等几乎全部业务，通常一个 war/jar、一个端口。  
- **微服务**：按业务边界拆成多个可独立部署的小服务，各自进程、可独立扩缩与发布。  

本项目对照：家里在用的 `store-inventory-backend` 是单体；学习仓拆成 platform / inventory / order / ai（+ gateway）。

### Q2：为什么要拆微服务？一定比单体好吗？

**答：**  
**为什么拆（收益）：**  

| 点 | 说明 |
|----|------|
| 独立演进 | 库存改动不必整包发版整站 |
| 独立扩容 | 下单峰值只加订单/库存实例 |
| 故障隔离 | 库存挂了，登录等平台能力仍可尝试可用（本项目已验证） |
| 团队边界 | 大团队可按服务分工 |

**代价：** 分布式变复杂（调用、超时、一致性、运维、排障）。  

**不是一定更好：** 人少、流量小、边界不清时，单体往往更合适。面试要会说「按规模与痛点选型，不是为了微服务而微服务」。

### Q3：什么是集群？和服务拆分有什么区别？

**答：**  

- **集群 / 多实例**：同一套程序跑多份（多台机器或多个进程），服务名相同，用来扛流量、做高可用。  
- **服务拆分（微服务）**：拆成**不同职责**的多个服务（不同服务名），如 order 与 inventory。  

可以同时存在：先拆出 `inventory-service`，再在两台机器上各部署一份 →「一种服务 + 多实例集群」。

```text
拆分：  order-service  ≠  inventory-service   （不同业务）
集群：  inventory-A    与  inventory-B         （同一业务，两份进程）
```

你以前 Eureka 两台服务器部署同一项目 = **集群（多实例）**，不一定等于已经拆微服务。

### Q4：为什么要集群（多实例）？一台不够吗？

**答：**  

| 原因 | 说明 |
|------|------|
| **高可用** | 一台挂了，另一台还能接请求 |
| **水平扩容** | 流量上来加机器，而不是无限加单机配置 |
| **滚动发布** | 可一台一台升级，降低停服窗口 |

单机足够且可接受单点故障时，开发环境一个实例即可（本机学习就是每服务 1 实例）。

### Q5：什么是负载均衡？为什么需要它？

**答：**  
负载均衡 = 在**多个能处理请求的目标**之间分配流量，避免打到单点。  

**为什么需要：** 有了集群却总打同一台，另一台闲着，集群就失去意义；也容易单点过载。  

常见位置：

| 类型 | 谁来选实例 | 例子 |
|------|------------|------|
| 服务端负载均衡 | Nginx / F5 / 云 LB / 网关 | 流量先到均衡器再转发 |
| 客户端负载均衡 | 调用方进程内选 | Spring Cloud LoadBalancer + Feign |

本项目：Feign/Gateway 用的是 **客户端负载均衡**（`lb://` / LoadBalancer）。

### Q6：负载均衡常见策略有哪些？本项目默认是什么？

**答：**  
常见：轮询、随机、加权轮询、最少连接、一致性哈希等。  
Spring Cloud LoadBalancer **默认轮询（Round Robin）**：第 1 次 A、第 2 次 B、第 3 次 A……  
不是默认随机；要随机需显式配置。

### Q7：网关、注册中心、负载均衡各自解决什么问题？为什么要「一整套」？

**答：**  

| 组件 | 解决的问题 | 不做会怎样 |
|------|------------|------------|
| **注册中心** | 服务在哪（动态地址） | 写死 IP，扩缩容痛苦 |
| **负载均衡** | 多个实例打哪一台 | 有集群也不会均匀/容错 |
| **网关** | 外部统一入口、路由 | 前端记一堆端口；鉴权/限流难集中 |
| **Feign 等** | 服务间怎么方便地调 | 手写 HTTP、难维护 |

单靠其中一样不够：只有注册中心不会自动帮你选实例；只有 Nginx 写死 upstream 也要人维护列表。Spring Cloud 把「发现 + 选实例 + 调用」串起来。

### Q8：结合本项目，从前端到库存完整走一遍（为什么这样设计）？

**答：**  

```text
浏览器
  → Vite/前端（只认一个后端入口）
  → Gateway:8080          （为什么：统一入口，按路径路由）
  → Nacos 查服务实例       （为什么：不写死 8081～8084）
  → LoadBalancer 选实例    （为什么：多实例时分摊；单实例也走同一套）
  → platform / order / …
       若下单锁库存：
       order → Feign → Nacos → LB → inventory
       （为什么：服务间不经网关，按服务名发现，库存可单独扩容）
```

**验证「为什么」的两个实验：**  

- 停 Gateway → 全站 API 不通 → 证明入口集中在网关  
- 只停库存 → 登录等仍可用、库存/锁库失败 → 证明拆分带来的隔离  

### Q9：水平扩展和垂直扩展有什么区别？

**答：**  

- **垂直扩展**：单机加 CPU/内存（scale up）  
- **水平扩展**：加机器/加实例（scale out）  

微服务 + 集群偏向水平扩展：库存热点就加 inventory 实例，而不必把整个单体机器配到很贵。

### Q10：高可用（HA）和负载均衡是一回事吗？

**答：**  
不是。  

- **负载均衡**：流量怎么分到多台  
- **高可用**：系统在部分故障时仍能提供服务  

负载均衡是实现 HA 的常用手段之一，但 HA 还包括健康检查、摘除故障节点、多机房、降级等。注册中心把不健康实例摘掉后，LoadBalancer 不再选它，二者配合才有「一台挂了请求还能成」。

---

## 2. Spring Cloud / SCA 基础概念

### Q11：Spring Cloud 和 Spring Cloud Alibaba 是什么关系？

**答：**  
Spring Cloud 提供微服务常见能力的规范与组件（如 OpenFeign、Gateway、LoadBalancer、服务发现抽象）。  
Spring Cloud Alibaba（SCA）是阿里的实现与增强，把 Nacos、Sentinel、Seata 等接到 Spring Cloud 体系。  
二者不是二选一：通常 **Boot + Cloud + SCA** 一起用。本项目：Cloud 管 Feign/Gateway，SCA 管 Nacos。

### Q12：为什么微服务常用注册中心？没有注册中心行不行？

**答：**  
要解决「A 怎么找到 B 的地址」。注册中心让服务自行上报、按名字发现，便于扩缩容与多实例。  
没有也可以：写死 URL、配置中心配地址、DNS、K8s Service、Nginx 反代等。  
Spring Cloud 教程默认教注册中心；上了 K8s 后发现常交给集群，Nacos 可主要做配置。

### Q13：注册、发现、调用分别是什么？举例子。

**答：**  

| | 含义 | 例子 |
|--|------|------|
| 注册 | 提供者把自己写入花名册 | inventory 启动后向 Nacos 报 IP:8082 |
| 发现 | 消费者按服务名查实例列表 | order 问：inventory-service 有哪些实例？ |
| 调用 | 选一个实例发 HTTP | Feign + LoadBalancer 发出真实请求 |

只注册不 Feign：控制台有服务，业务仍可能写死 localhost。

### Q14：Eureka 和 Nacos 有什么区别？

**答：**  

| | Eureka | Nacos |
|--|--------|--------|
| 形态 | 常是一个 Eureka Server 的 Boot 项目 | 独立中间件（解压/Docker 启动） |
| 能力 | 主要做注册发现 | 注册发现 + **配置中心** |
| 现状 | Netflix 不积极演进 | 国内主流，与 SCA 绑定紧 |

本项目用 Nacos Server 2.5.2 + SCA 客户端。

---

## 3. Nacos 注册发现

### Q15：服务注册到 Nacos 要改哪些地方？

**答：**  
1. 依赖 `spring-cloud-starter-alibaba-nacos-discovery`  
2. 配置 `spring.application.name`、`spring.cloud.nacos.discovery.server-addr`  
3. 启动类 `@EnableDiscoveryClient`（新版本可自动，面试建议能说出意图）  

不消费其它服务时，一般**不需要** Feign。

### Q16：服务名、分组、命名空间、实例分别是什么？

**答：**  

- **服务名**：`spring.application.name`，调用时用的逻辑名（如 `inventory-service`）  
- **实例**：同一服务名下的一个进程（IP:端口）；多机部署则多实例  
- **分组**：默认 `DEFAULT_GROUP`，同一命名空间内再分类  
- **命名空间**：环境隔离（dev/test/prod），本机常用 `public`  

### Q17：两台服务器部署同一套库存服务，在注册中心是什么？

**答：**  
**同一服务的多实例（水平扩展 / 高可用）**，不是拆出两个微服务。  
控制台：服务数=1（inventory-service），实例数=2。  
调用方通过 LoadBalancer 在实例间分配请求。

### Q18：进程停了，Nacos 里为什么还能看到服务？

**答：**  
客户端靠心跳维持；进程被杀后要等心跳超时才会标不健康或摘除，有延迟。  
IDEA 优雅关闭未结束时进程可能仍存活。验证时看端口是否释放、详情里健康状态。

---

## 4. OpenFeign 与 LoadBalancer

### Q19：什么时候需要 Feign？和 RestTemplate 写死 URL 比有什么好处？

**答：**  
本服务**要调用**其它服务时才需要 Feign（+ LoadBalancer）。  
好处：按**服务名**调用，地址从注册中心动态获取，多实例自动负载；换机器少改配置。  
本项目：order、platform 调库存用 Feign；inventory 主要被调，可不配 Feign。

### Q20：`@FeignClient(name="inventory-service", path="/api")` 里两个参数什么意思？

**答：**  

- `name`：必须与对方 `spring.application.name` 一致，用于服务发现  
- `path`：请求路径前缀；对方有 `context-path=/api`，而 Nacos 只登记 host:port，所以要补 `/api`  

### Q21：LoadBalancer 是什么？多实例时默认怎么选？

**答：**  
客户端负载均衡：在调用方进程内，从发现到的实例列表里**选一个**再发请求。  
Spring Cloud LoadBalancer **默认轮询（Round Robin）**，不是默认随机。  
报错 `Load balancer does not contain an instance for the service xxx` = 列表为空（服务未注册或已全部下线）。

### Q22：Feign 调不通时，报 `http://inventory-service/...` 是不是配错了？

**答：**  
不是。这是**逻辑服务名**形式的地址；真正 IP 在选到实例后才用。  
若仍出现 `http://localhost:8082/...`，多半还在跑旧 RestTemplate 代码，需重启消费方。

### Q23：服务间调用要不要走 Gateway？

**答：**  
一般**不需要**。Gateway 是给浏览器/外部的统一入口；服务间用 Feign 直连（经注册中心发现）即可，少一跳、更清晰。  
本项目：前端 → Gateway；order/platform → inventory 走 Feign。

---

## 5. Gateway

### Q24：为什么要引入 API 网关？

**答：**  
统一入口、按路径路由、后续可集中做鉴权/限流/日志。  
前端只需打一个地址（本机 8080），不必记 8081～8084。

### Q25：Gateway 为什么不要引入 `spring-boot-starter-web`？

**答：**  
Spring Cloud Gateway 基于 **WebFlux/Netty**；再引入 Web（Servlet/Tomcat）会栈冲突。  
只需 `spring-cloud-starter-gateway` + 发现 + LoadBalancer。

### Q26：`lb://inventory-service` 是什么意思？

**答：**  
`lb` = load balancer。Gateway 用服务名从注册中心取实例并负载转发，而不是写死 `http://ip:port`。

### Q27：停 Gateway 和停库存，现象有什么不同？用来验证什么？

**答：**  

| 操作 | 现象 | 说明 |
|------|------|------|
| 停 Gateway | 无法登录等，前端 API 全挂 | 统一入口断了 |
| 只停库存 | 库存相关失败；登录/平台其它功能仍可用 | 路由隔离正确 |

---

## 6. 版本与工程实践

### Q28：Boot / Cloud / SCA 版本为什么要对齐？

**答：**  
三者有官方兼容表；乱配易依赖冲突、启动失败。  
本项目：Boot 3.2.5 + Cloud 2023.0.1 + SCA 2023.0.1.0。

### Q29：父 POM 引入 BOM 后子模块还要写版本号吗？

**答：**  
`dependencyManagement` 管版本后，子模块声明依赖**通常不写版本**。  
注意：只 import BOM **不会**自动把 starter 加进 classpath，子模块仍要显式写 dependency。

### Q30：SCA BOM 导致 Spring AI 编译失败怎么处理过？

**答：**  
SCA 可能管理较旧的 `spring-ai-core`。Maven 对同坐标以**先声明的 BOM**为准，故把 **Spring AI BOM 放在 SCA BOM 之前**，保证项目使用 1.0.0-M6。

---

## 待补充（后续接入再写）

- [ ] Nacos Config：DataId / Group、动态刷新  
- [ ] Sentinel：限流熔断降级  
- [ ] Seata / 最终一致性  
- [ ] 网关统一鉴权与跨域  
- [ ] 配置加密、Nacos 鉴权  
- [ ] 与 K8s Service 发现的对比落地  

（做完对应步骤后往本文件追加 Q&A 即可。）
