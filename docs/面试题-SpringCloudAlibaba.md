# 面试题 · Spring Cloud Alibaba（当前进度）

> 覆盖本仓库已落地内容：Nacos 注册发现、OpenFeign、LoadBalancer、Gateway。  
> **后续边做边补**（Config / Sentinel / Seata 等）。  
> 实操笔记：[`治理层接入笔记-Nacos-Feign-Gateway.md`](./治理层接入笔记-Nacos-Feign-Gateway.md)

---

## 1. 基础概念

### Q1：Spring Cloud 和 Spring Cloud Alibaba 是什么关系？

**答：**  
Spring Cloud 提供微服务常见能力的规范与组件（如 OpenFeign、Gateway、LoadBalancer、服务发现抽象）。  
Spring Cloud Alibaba（SCA）是阿里的实现与增强，把 Nacos、Sentinel、Seata 等接到 Spring Cloud 体系。  
二者不是二选一：通常 **Boot + Cloud + SCA** 一起用。本项目：Cloud 管 Feign/Gateway，SCA 管 Nacos。

### Q2：为什么微服务常用注册中心？没有注册中心行不行？

**答：**  
要解决「A 怎么找到 B 的地址」。注册中心让服务自行上报、按名字发现，便于扩缩容与多实例。  
没有也可以：写死 URL、配置中心配地址、DNS、K8s Service、Nginx 反代等。  
Spring Cloud 教程默认教注册中心；上了 K8s 后发现常交给集群，Nacos 可主要做配置。

### Q3：注册、发现、调用分别是什么？举例子。

**答：**  

| | 含义 | 例子 |
|--|------|------|
| 注册 | 提供者把自己写入花名册 | inventory 启动后向 Nacos 报 IP:8082 |
| 发现 | 消费者按服务名查实例列表 | order 问：inventory-service 有哪些实例？ |
| 调用 | 选一个实例发 HTTP | Feign + LoadBalancer 发出真实请求 |

只注册不 Feign：控制台有服务，业务仍可能写死 localhost。

### Q4：Eureka 和 Nacos 有什么区别？

**答：**  

| | Eureka | Nacos |
|--|--------|--------|
| 形态 | 常是一个 Eureka Server 的 Boot 项目 | 独立中间件（解压/Docker 启动） |
| 能力 | 主要做注册发现 | 注册发现 + **配置中心** |
| 现状 | Netflix 不积极演进 | 国内主流，与 SCA 绑定紧 |

本项目用 Nacos Server 2.5.2 + SCA 客户端。

---

## 2. Nacos 注册发现

### Q5：服务注册到 Nacos 要改哪些地方？

**答：**  
1. 依赖 `spring-cloud-starter-alibaba-nacos-discovery`  
2. 配置 `spring.application.name`、`spring.cloud.nacos.discovery.server-addr`  
3. 启动类 `@EnableDiscoveryClient`（新版本可自动，面试建议能说出意图）  

不消费其它服务时，一般**不需要** Feign。

### Q6：服务名、分组、命名空间、实例分别是什么？

**答：**  

- **服务名**：`spring.application.name`，调用时用的逻辑名（如 `inventory-service`）  
- **实例**：同一服务名下的一个进程（IP:端口）；多机部署则多实例  
- **分组**：默认 `DEFAULT_GROUP`，同一命名空间内再分类  
- **命名空间**：环境隔离（dev/test/prod），本机常用 `public`  

### Q7：两台服务器部署同一套库存服务，在注册中心是什么？

**答：**  
**同一服务的多实例（水平扩展 / 高可用）**，不是拆出两个微服务。  
控制台：服务数=1（inventory-service），实例数=2。  
调用方通过 LoadBalancer 在实例间分配请求。

### Q8：进程停了，Nacos 里为什么还能看到服务？

**答：**  
客户端靠心跳维持；进程被杀后要等心跳超时才会标不健康或摘除，有延迟。  
IDEA 优雅关闭未结束时进程可能仍存活。验证时看端口是否释放、详情里健康状态。

---

## 3. OpenFeign 与 LoadBalancer

### Q9：什么时候需要 Feign？和 RestTemplate 写死 URL 比有什么好处？

**答：**  
本服务**要调用**其它服务时才需要 Feign（+ LoadBalancer）。  
好处：按**服务名**调用，地址从注册中心动态获取，多实例自动负载；换机器少改配置。  
本项目：order、platform 调库存用 Feign；inventory 主要被调，可不配 Feign。

### Q10：`@FeignClient(name="inventory-service", path="/api")` 里两个参数什么意思？

**答：**  

- `name`：必须与对方 `spring.application.name` 一致，用于服务发现  
- `path`：请求路径前缀；对方有 `context-path=/api`，而 Nacos 只登记 host:port，所以要补 `/api`  

### Q11：LoadBalancer 是什么？多实例时默认怎么选？

**答：**  
客户端负载均衡：在调用方进程内，从发现到的实例列表里**选一个**再发请求。  
Spring Cloud LoadBalancer **默认轮询（Round Robin）**，不是默认随机。  
报错 `Load balancer does not contain an instance for the service xxx` = 列表为空（服务未注册或已全部下线）。

### Q12：Feign 调不通时，报 `http://inventory-service/...` 是不是配错了？

**答：**  
不是。这是**逻辑服务名**形式的地址；真正 IP 在选到实例后才用。  
若仍出现 `http://localhost:8082/...`，多半还在跑旧 RestTemplate 代码，需重启消费方。

### Q13：服务间调用要不要走 Gateway？

**答：**  
一般**不需要**。Gateway 是给浏览器/外部的统一入口；服务间用 Feign 直连（经注册中心发现）即可，少一跳、更清晰。  
本项目：前端 → Gateway；order/platform → inventory 走 Feign。

---

## 4. Gateway

### Q14：为什么要引入 API 网关？

**答：**  
统一入口、按路径路由、后续可集中做鉴权/限流/日志。  
前端只需打一个地址（本机 8080），不必记 8081～8084。

### Q15：Gateway 为什么不要引入 `spring-boot-starter-web`？

**答：**  
Spring Cloud Gateway 基于 **WebFlux/Netty**；再引入 Web（Servlet/Tomcat）会栈冲突。  
只需 `spring-cloud-starter-gateway` + 发现 + LoadBalancer。

### Q16：`lb://inventory-service` 是什么意思？

**答：**  
`lb` = load balancer。Gateway 用服务名从注册中心取实例并负载转发，而不是写死 `http://ip:port`。

### Q17：停 Gateway 和停库存，现象有什么不同？用来验证什么？

**答：**  

| 操作 | 现象 | 说明 |
|------|------|------|
| 停 Gateway | 无法登录等，前端 API 全挂 | 统一入口断了 |
| 只停库存 | 库存相关失败；登录/平台其它功能仍可用 | 路由隔离正确 |

---

## 5. 版本与工程实践

### Q18：Boot / Cloud / SCA 版本为什么要对齐？

**答：**  
三者有官方兼容表；乱配易依赖冲突、启动失败。  
本项目：Boot 3.2.5 + Cloud 2023.0.1 + SCA 2023.0.1.0。

### Q19：父 POM 引入 BOM 后子模块还要写版本号吗？

**答：**  
`dependencyManagement` 管版本后，子模块声明依赖**通常不写版本**。  
注意：只 import BOM **不会**自动把 starter 加进 classpath，子模块仍要显式写 dependency。

### Q20：SCA BOM 导致 Spring AI 编译失败怎么处理过？

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
