# 治理层接入笔记（Nacos / Feign / Gateway）

> 对应学习步骤 **第 1～8 步**（含 Nacos Config 演示）。  
> 业务拆分见 [`微服务拆分-起步.md`](./微服务拆分-起步.md)；面试题见 [`面试题-SpringCloudAlibaba.md`](./面试题-SpringCloudAlibaba.md)。

## 一句话总览

```text
本机 Nacos → 父 POM 锁版本 → 四服务注册 → 弄清「注册≠调用」
→ order/platform 用 Feign 按服务名调库存 → Gateway 统一入口 → 前端只打 8080
→ order 演示 Nacos Config（远程配置 + 可刷新）
```

| 组件 | 端口 / 角色 |
|------|-------------|
| Nacos Server | 8848（独立安装，非业务模块） |
| gateway-service | **8080** 统一入口 |
| platform-service | 8081 |
| inventory-service | 8082 |
| order-service | 8083 |
| ai-service | 8084 |

启动顺序建议：**Nacos → 四业务 → Gateway → 前端**。

---

## 第 0 步：本机安装 Nacos（前置）

- 下载 **2.x** 发行包（学习用过 2.5.2），解压到无中文路径  
- JDK 17 需配 `JAVA_HOME`；若遇 `InaccessibleObjectException`，启动加：  
  `--add-opens=java.base/java.io=ALL-UNNAMED`  
- 单机启动：`startup.cmd -m standalone`  
- 控制台：http://127.0.0.1:8848/nacos （默认 nacos/nacos）  
- **Eureka vs Nacos**：Eureka 常是一个 Spring Boot「注册中心项目」；Nacos 是独立中间件，业务仓只做 Client  

---

## 第 1 步：父 POM 引入 Spring Cloud + SCA BOM

**只改** `store-inventory-backend-ms/pom.xml`，业务行为不变。

版本对齐（官方推荐组合）：

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.2.5 |
| Spring Cloud | 2023.0.1 |
| Spring Cloud Alibaba | 2023.0.1.0 |

```xml
<!-- dependencyManagement 中 import -->
spring-cloud-dependencies
spring-cloud-alibaba-dependencies
```

要点：

- `dependencyManagement` = 价目表，**不等于**已经引入依赖  
- BOM 导入顺序：同坐标「先声明优先」。SCA 会带旧版 Spring AI，故 **Spring AI BOM 须排在 SCA 之前**  

---

## 第 2～3 步：服务注册到 Nacos

每个要上户口的服务改三处：

1. **pom**：`spring-cloud-starter-alibaba-nacos-discovery`  
2. **yml**：`spring.application.name` + `spring.cloud.nacos.discovery.server-addr`  
3. **启动类**：`@EnableDiscoveryClient`  

先做 platform，再复制到 inventory / order / ai。

控制台：**服务管理 → 服务列表** 应看到 4 个服务、各 1 健康实例。

注意：

- 手动停进程后，Nacos **不会立刻摘掉**（心跳超时才更新）  
- IDEA「Waiting for process detach」= 优雅关闭未结束，进程可能还活着  

---

## 第 4 步：注册 ≠ 发现 ≠ 调用（概念）

| 概念 | 谁做 | 例子 |
|------|------|------|
| **注册** | 提供者上报 | inventory 告诉 Nacos：我在 IP:8082 |
| **发现** | 消费者查询 | order 问 Nacos：inventory-service 在哪？ |
| **调用** | 选实例后发 HTTP | Feign + LoadBalancer |

只注册、不 Feign → 控制台有名单，业务仍可写死 `localhost`。  
多实例时：同一 `spring.application.name`，实例数 > 1；LoadBalancer **默认轮询**，不是默认随机。

不用注册中心也能「微服务」（写死 URL / DNS / K8s Service 等），Spring Cloud 学习默认走注册中心。

---

## 第 5 步：order → inventory 改 Feign

消费方额外依赖：

```xml
spring-cloud-starter-openfeign
spring-cloud-starter-loadbalancer
```

```java
@FeignClient(name = "inventory-service", path = "/api")
```

- `name` = 对方 `spring.application.name`  
- `path=/api` = 对方 `context-path`（Nacos 只登记 host:port）  
- 启动类：`@EnableFeignClients`  
- 去掉 `inventory.service.base-url` 与 RestTemplate  

验收：停掉 inventory → 报类似：

```text
Load balancer does not contain an instance for the service inventory-service
```

且 URL 形态为 `http://inventory-service/api/...`（服务名，不是 localhost:8082）。

---

## 第 6 步：platform → inventory 改 Feign

与第 5 步同套路：`InventoryStockFeignClient` + 门面 `InventoryStockClient`。  
场景：新增商品 `initStock`、删除前 `getStockSnapshot`。  
看板同库直读 Stock Mapper 仍是技术债，未纳入本次。

---

## 第 7 步：Gateway + 前端统一入口

新建 `gateway-service`（**不要**再引 `spring-boot-starter-web`）：

| 路径 | 转发 |
|------|------|
| `/api/inventory/**` | `lb://inventory-service` |
| `/api/order/**` | `lb://order-service` |
| `/api/ai/**` | `lb://ai-service` |
| `/upload/**` | platform（rewrite → `/api/upload/**`） |
| `/api/**` | `lb://platform-service` |

前端 Vite：`/api`、`/upload` 只代理到 `http://localhost:8080`。

调用链：

```text
浏览器 → Vite:5173 → Gateway:8080 → Nacos 查服务 → 业务实例
服务间 Feign（order/platform→inventory）不经过 Gateway
```

验收：

- 停 Gateway → 无法登录（入口断）  
- 只停库存 → 库存相关失败，登录/平台其它功能仍可用  

---

## 第 8 步：Nacos Config（先在 order-service 演示）

**注册发现**管「服务在哪」；**配置中心**管「配置从哪来、能否热更新」。

### 代码改动（order-service）

1. pom：`spring-cloud-starter-alibaba-nacos-config`（**不**再依赖 bootstrap）  
2. `application.yml`：  
   - `spring.cloud.nacos.server-addr`  
   - `spring.config.import: nacos:order-service.yaml?group=DEFAULT_GROUP&refreshEnabled=true`  
3. `OrderNacosDemoProperties`：`@Value` + `@RefreshScope`  
4. `/order/ping` 回显 `nacosDemoMessage`  

本地默认：`app.nacos-demo-message: local-default`。  

> 不用 `optional:`：控制台没有该配置时启动会失败，避免静默退回本地默认值。  
> 若暂时还没建配置、又想先启动：可改成 `optional:nacos:order-service.yaml?...`。

### 你在 Nacos 控制台要建的配置

1. **配置管理 → 配置列表 → 创建配置**  
2. 填写：

| 项 | 值 |
|----|-----|
| Data ID | `order-service.yaml`（必须与 import 一致） |
| Group | `DEFAULT_GROUP` |
| 配置格式 | YAML |
| 配置内容 | 见下方 |

```yaml
app:
  nacos-demo-message: from-nacos-config
```

3. 发布 → **重启 order-service**（第一次建议重启；之后改值可试热刷新）  
4. 访问（经网关或直连均可）：

```text
GET http://localhost:8080/api/order/ping
# 或 http://localhost:8083/api/order/ping
```

期望：`nacosDemoMessage` = `from-nacos-config`（不是 `local-default`）。

5. **热刷新体验**：把 Nacos 里改成 `from-nacos-refresh`，发布后再打 ping（`@RefreshScope` 应更新，一般无需重启）。

### 概念对照

| 概念 | 含义 |
|------|------|
| Data ID | 配置文件名，常为 `服务名.yaml` |
| Group | 分组，默认 `DEFAULT_GROUP` |
| 命名空间 | 环境隔离（本机用 public） |
| refreshEnabled | 是否监听变更并刷新 |

---

## 当前未做（后续可选）

- 其它服务也迁配置到 Nacos、共享配置  
- 网关鉴权统一、Sentinel、Seata、Docker/K8s、完整 CI/CD  

---

## 本机联调检查清单

1. Nacos 控制台能开，服务列表有 gateway + 四业务  
2. IDEA 里进程用最新代码重启  
3. 前端改过 `vite.config.ts` 后需重启 `npm run dev`  
4. 验证「停谁挂谁」时，先确认端口真正释放  
5. 第 8 步：Nacos 已建 `order-service.yaml`，`/order/ping` 能看到远程文案  
