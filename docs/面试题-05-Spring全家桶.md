# 第 5 批：Spring 全家桶面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 互补：[并发/Spring基础-面试.md](./并发/Spring基础-面试.md) · [RBAC权限设计.md](./RBAC权限设计.md)  
> 本批共 **8 题** · 下一批：并发编程（**公共厕所类比**）

---

## 题 1：IoC / DI 与 Bean 生命周期

================

## 面试问题：

什么是 IoC 和 DI？Spring Bean 的生命周期大致有哪些阶段？`@Autowired` 和构造器注入哪个更推荐？

## 考察点：

- 控制反转 vs 依赖注入
- 单例 Bean 创建流程（Instantiation → 属性填充 → 初始化）
- 构造器注入不可变、易测试
- 结合项目 Service/Feign 注入

## 标准答案：

**IoC（控制反转）**：对象的创建与依赖关系由 **Spring 容器** 管理，而不是业务类里 `new`。

**DI（依赖注入）**：容器把依赖 **注入** 到对象（构造器、setter、字段）。

**Bean 生命周期（简化）**：

1. 实例化（构造器）
2. 属性赋值（`@Autowired`）
3. `BeanNameAware` 等 Aware 回调
4. `BeanPostProcessor.postProcessBeforeInitialization`
5. `@PostConstruct` / `InitializingBean.afterPropertiesSet`
6. `BeanPostProcessor.postProcessAfterInitialization`（AOP 代理常在此生成）
7. 使用
8. 容器关闭 → `@PreDestroy` / `DisposableBean`

**注入方式推荐**：

| 方式 | 评价 |
|------|------|
| **构造器注入** | **首选**：依赖 final、必填、易单测、避免空指针 |
| Setter | 可选依赖 |
| 字段 `@Autowired` | 简便但 **难测、隐藏依赖**，阿里规约不推荐字段注入 |

**循环依赖**：三级缓存解决 **单例 + 字段/setter**；**构造器循环** 无法解决，应重构。

## 通俗理解：

IoC 像 **公司统一采购**：你要的 Feign Client、Mapper 由 Spring「后勤部」配好送来，不用自己 new。  
Bean 生命周期像 **员工入职**：体检（实例化）→ 领工牌（注入）→ 培训（初始化）→ 上班 → 离职（销毁）。

## 项目结合：

```java
@Service
@RequiredArgsConstructor  // Lombok 生成构造器注入
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final InventoryFeignClient inventoryFeignClient;
}
```

- **order-service** 依赖 Feign 调库存，构造器注入 **强制依赖存在**，启动时就能发现 Feign 配置错误
- **AiChatServiceImpl** 注入 `ChatModel`、`ChatSessionStore`，便于 Mock 单测
- 避免 **Controller 里 @Autowired 字段** 一堆，改构造器更清晰

## 面试官追问：

1. 三级缓存是哪三级？
2. prototype Bean 能注入 singleton 吗？反过来呢？
3. `@Lazy` 干什么用？

## 高级回答：

- **三级缓存**：singletonObjects、earlySingletonObjects、singletonFactories（暴露早期引用解决循环）。
- **prototype → singleton**：可以；**singleton → prototype**： prototype 只在注入时创建一次，**不是** 每次请求新 prototype，要 `@Lookup` 或 `ObjectProvider`。
- **@Lazy**：延迟初始化 Bean，加快启动；Feign Client 一般不必 lazy。
- 10 年答法：**「项目统一构造器 + final；循环依赖说明设计有问题，优先拆 Service」**。

================

---

## 题 2：Spring Boot 自动配置原理

================

## 面试问题：

Spring Boot 如何做到「引入 starter 就能用」？`@SpringBootApplication` 等价于什么？如何排除某个自动配置？

## 考察点：

- `@EnableAutoConfiguration` + `spring.factories` / `AutoConfiguration.imports`
- 条件注解 `@ConditionalOnClass` 等
- 自定义 starter 思路（加分）

## 标准答案：

**核心机制**：

1. `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
2. **AutoConfiguration** 类在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（Boot 3）或 `spring.factories`（Boot 2）注册
3. 自动配置类大量 `@ConditionalOnXxx`：classpath 有类、有配置项、缺少某 Bean 时才生效
4. **外部化配置** `application.yml` 绑定 `@ConfigurationProperties`

**排除自动配置**：

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
// 或 yml: spring.autoconfigure.exclude: ...
```

**调试**：启动加 `--debug` 看 **Condition Evaluation Report**。

**与 Spring 关系**：Boot = Spring Framework + **约定优于配置** + 内嵌 Tomcat + Actuator 等。

## 通俗理解：

自动配置像 **精装房**：检测到你有 JDBC 驱动，就自动配好 HikariCP 和 DataSource；你只在 yml 里写 url/username。

## 项目结合：

- 引入 **`spring-cloud-starter-alibaba-nacos-discovery`** → 自动配 Nacos 注册
- **`spring-boot-starter-security`** → SecurityFilterChain 自动配，你在 `@Configuration` 里定制 JWT
- **`spring-ai-openai-spring-boot-starter`**（通义兼容）→ `ChatModel` Bean 自动就绪
- **多模块**：`ai-service` 排除不需要的 DataSource 自动配（若纯 RPC 无库）要显式 exclude，避免启动失败

## 面试官追问：

1. `@ConditionalOnMissingBean` 有什么用？
2. 自动配置的加载顺序？
3. Spring Boot 3 和 2 在 jakarta 上有什么变化？

## 高级回答：

- **OnMissingBean**：用户自定义 Bean **覆盖** 默认，扩展点标准做法。
- **顺序**：`@AutoConfigureOrder`、`@DependsOn`；Security 要在 MVC 前等。
- **jakarta.*` 替代 `javax.*`**（Servlet、Persistence）；进销存若 JDK17 + Boot 3 要统一依赖。
- 10 年答法：**「排错看 Condition Report；生产不靠盲目 @Import，靠 starter + yml + 少量 @Configuration」**。

================

---

## 题 3：Spring MVC 请求处理流程

================

## 面试问题：

一个 HTTP 请求进入 Spring Boot 应用，到 Controller 返回 JSON，经过哪些组件？`@RestController` 和 `@Controller` 区别？

## 考察点：

- DispatcherServlet、HandlerMapping、HandlerAdapter
- 拦截器 vs Filter vs Gateway Filter
- 统一返回与异常处理

## 标准答案：

**流程（Servlet 栈）**：

1. **Tomcat** 接收 HTTP
2. **Filter 链**（Servlet 规范）：CharacterEncoding、**Spring Security FilterChain**（JWT 校验在此）
3. **DispatcherServlet**  front controller
4. **HandlerMapping** 找 `@RequestMapping` 方法
5. **HandlerInterceptor** `preHandle`（可选）
6. **HandlerAdapter** 调用 Controller（参数解析 `@RequestBody`、`@PathVariable`）
7. **Controller** 返回对象
8. **HttpMessageConverter**（Jackson）→ JSON
9. **Interceptor** `postHandle` / `afterCompletion`
10. 响应客户端

**@RestController** = `@Controller` + `@ResponseBody`（方法返回值直接写 body）。

**对比 Gateway**：Gateway 是 **WebFlux + Netty**，在进销存里是 **入口路由**；到 order-service 后仍是 **Spring MVC**。

## 通俗理解：

DispatcherServlet 像 **总机**：看 URL 转接到「订单部 Controller」，参数帮拆好，返回值帮你 **包装成 JSON 快递**。

## 项目结合：

- **`POST /api/orders`**：`OrderController` → `OrderService` → Mapper + Feign
- **JWT Filter** 在 DispatcherServlet **之前**，未登录直接 401，进不了 Controller
- **统一 `Result<T>`**：可用 `ResponseBodyAdvice` 包装（若未全局包装）
- **`@RestControllerAdvice`** 捕获 `BizException` → `{code, message}`

## 面试官追问：

1. Filter 和 Interceptor 区别？
2. `@RequestBody` 怎么绑定？
3. 跨域 CORS 在哪配？

## 高级回答：

- **Filter**：Servlet 规范，可拦静态资源；**Interceptor**：Spring MVC 内，知 Handler 信息。
- **RequestBody**：`RequestResponseBodyMethodProcessor` + Jackson `ObjectMapper`；日期格式靠 `@JsonFormat` 或全局配置。
- **CORS**：开发 Vue 代理；生产 **Nginx 或 Gateway** `globalcors`；Security 也要 permit OPTIONS。
- 10 年答法：**「401 查 Security Filter；404 查 Mapping；415 查 Content-Type 与 Converter」**。

================

---

## 题 4：AOP 与 @Transactional 原理

================

## 面试问题：

Spring AOP 实现原理？JDK 动态代理和 CGLIB 区别？`@Transactional` 什么时候会失效？

## 考察点：

- 代理对象、切面织入
- 事务传播、rollbackFor
- 经典失效场景（必考）

## 标准答案：

**AOP 原理**：

- Spring 为 Bean 创建 **代理对象**，在目标方法前后执行切面（@Before/@Around 等）
- **有接口** 默认 **JDK 动态代理**（基于接口）
- **无接口** 或 `proxyTargetClass=true` → **CGLIB** 子类代理

**@Transactional**：

- 基于 AOP，在方法前后 **开启/提交/回滚** 连接（PlatformTransactionManager）
- **传播行为**：REQUIRED（默认，加入或新建）、REQUIRES_NEW（挂起当前开新事务）、NESTED 等
- **rollbackFor**：默认只回滚 **RuntimeException**；checked 异常要显式 `rollbackFor = Exception.class`

**失效场景（高频）**：

1. **同类自调用**（绕过代理）
2. 方法 **非 public**
3. **异常被吞** 或未满足 rollback 规则
4. 数据库引擎 **不支持事务**（MyISAM）
5. **多数据源** 未配 `@Transactional` 指定 tm
6. **传播 REQUIRES_NEW** 误用导致部分提交

## 通俗理解：

AOP 像 **给方法套透明保护壳**：壳上负责开事务、记日志；你写的 Service 只管业务。  
自调用失效像 **没经过前台自己改库存**，保护壳没套上。

## 项目结合：

- **下单写订单表 + 写本地流水**：`@Transactional(rollbackFor = Exception.class)` 同库有效
- **调 Feign 扣库存**：Feign **不在同一事务**；inventory 挂了 order 回滚 **挡不住** 已提交的远程调用 → 要补偿
- **操作日志 AOP**：`@Around` 记录 `@Log` 注解方法，异常也要 `afterCompletion` 写失败
- **禁止** 在 Controller 上加 `@Transactional`

## 面试官追问：

1. REQUIRED 和 REQUIRES_NEW 区别？
2. 只读事务 `readOnly=true` 原理？
3. 事务隔离级别在 Spring 怎么设？

## 高级回答：

- **REQUIRES_NEW**：独立提交，外层回滚 **不影响** 内层已提交（适合独立审计日志，慎用）。
- **readOnly**：MySQL 提示无写；Hibernate flush 模式；PG 可路由只读副本。
- **隔离级别**：`@Transactional(isolation = Isolation.READ_COMMITTED)` 少用，默认 DB 即可。
- 10 年答法：**「跨服务无分布式事务时，本地 @Transactional 只保证本库；失效排查从 public、自调用、rollbackFor 三项开始」**。

================

---

## 题 5：Spring Security + JWT

================

## 面试问题：

Spring Security 过滤器链大致顺序？JWT 无状态认证怎么接入？`@PreAuthorize` 和 URL 授权区别？

## 考察点：

- SecurityFilterChain（Boot 3）vs WebSecurityConfigurerAdapter（旧）
- Token 解析、Authentication 放入 SecurityContext
- 与 Gateway 分工

## 标准答案：

**过滤器链（概念顺序）**：

DisableUrlSession → Logout → **JwtAuthenticationFilter**（自定义）→ UsernamePassword（可关）→ Authorization → ExceptionTranslation → FilterSecurityInterceptor

**JWT 流程**：

1. 登录 `POST /api/auth/login` **permitAll**，校验密码，签发 JWT（含 userId、roles、exp）
2. 后续请求 Header `Authorization: Bearer <token>`
3. **JwtFilter** 解析 → 构建 `UsernamePasswordAuthenticationToken` → `SecurityContextHolder`
4. **授权**：URL `requestMatchers("/api/admin/**").hasRole("ADMIN")` 或方法 `@PreAuthorize("hasAuthority('goods:edit')")`

**无状态**：`SessionCreationPolicy.STATELESS`，不用 HttpSession。

**Gateway vs 服务**：

- Gateway：可选 **统一验签**、限流、路由
- 微服务：仍建议 **各自验 JWT** 或信任 Gateway 转发的 **已认证 Header**（内网要防伪造）

## 通俗理解：

Security 像 **门卫 + 工牌系统**：登录发 JWT 工牌；每次进门刷工牌；`@PreAuthorize` 像 **进仓库还要扫权限码**。

## 项目结合：

- **RBAC**：用户-角色-权限；权限码 `goods:add` 绑 `@PreAuthorize`
- **操作日志** 取 `SecurityContext` 当前用户 id
- **Feign 传递用户**：`RequestInterceptor` 把 Authorization 或内部 **用户上下文 Header** 传给 inventory（只读接口）
- **401 vs 403**：未登录 / 已登录无权限，前端分别跳登录 / 提示

## 面试官追问：

1. JWT 如何刷新？refresh token 放哪？
2. 登出后 JWT 还能用怎么办？
3. CSRF 为什么 API 常 disable？

## 高级回答：

- **Refresh Token**：短 access + 长 refresh，refresh 存 **HttpOnly Cookie** 或 Redis 白名单。
- **登出**：Redis **token 黑名单**（jti + TTL）或版本号踢人。
- **CSRF**：无 Cookie Session 的 Bearer JWT **无 CSRF 风险**；若 Cookie 存 token 则要 CSRF 防护。
- 10 年答法：**「JWT 只存声明，权限变更靠短 TTL + Redis 黑名单；敏感操作二次校验」**。

================

---

## 题 6：MyBatis 与 Spring 集成

================

## 面试问题：

MyBatis 和 Hibernate/JPA 区别？`#{}` 和 `${}` 区别？MyBatis 一级、二级缓存要注意什么？

## 考察点：

- SQL 可控 vs ORM 自动化
- SQL 注入
- 分页插件、Mapper 扫描

## 标准答案：

**MyBatis vs JPA**：

| | MyBatis | JPA/Hibernate |
|---|---------|---------------|
| SQL | 手写/半自动，**可控** | 自动生成，复杂 SQL 难 |
| 学习 | SQL 友好 | 对象关系映射 |
| 场景 | 报表、复杂 join、国内 CRUD 多 | 领域模型驱动 |

**#{} vs ${}**：

- **`#{}`**：预编译 **占位符**，防 SQL 注入，值带引号
- **`${}`**：**字符串替换**，用于 **表名/列名/ORDER BY** 动态部分，**必须白名单校验**

**缓存**：

- **一级**：SqlSession 级，默认开；同一 Session 内重复查命中
- **二级**：Mapper namespace 级，需 `<cache/>`；**多表 join 脏读风险**，生产常 **关二级**，用 Redis

**Spring 集成**：`@MapperScan` → Mapper 接口 JDK 动态代理 → SqlSessionTemplate 执行 XML/注解 SQL。

## 通俗理解：

MyBatis 像 **自己写 SQL 单据**，Spring 帮你管数据库连接和事务；`${}` 像 **把用户输入直接贴进 SQL**，贴错就 injection。

## 项目结合：

- **库存扣减 XML**：

```xml
<update id="deduct">
  UPDATE stock SET usable_qty = usable_qty - #{qty}
  WHERE goods_id = #{goodsId} AND usable_qty >= #{qty}
</update>
```

- **商品动态查询**：`<if test="name != null">` 拼 WHERE，name 用 `#{}`
- **ORDER BY 动态列**：禁止直接 `${sortField}`，用 **枚举白名单** `SortColumn.name.name()`
- **MyBatis-Plus**：简化 CRUD；复杂报表仍 XML
- **多数据源**：ai 库与业务库 `@DS("master")`（若用 dynamic-datasource）

## 面试官追问：

1. N+1 查询是什么？MyBatis 怎么避免？
2. PageHelper 原理？
3. `@Transactional` 和 SqlSession 关系？

## 高级回答：

- **N+1**：查订单列表再循环查明细 → 改 **join 一次查** 或 **批量 in 查询**。
- **PageHelper**：ThreadLocal 拦截下一个 SELECT 加 limit，**只对紧跟的一条 SQL 生效**。
- **事务**：同一 `@Transactional` 内共用一个 Connection，一级缓存有效；事务结束 Session 关。
- 10 年答法：**「进销存复杂 SQL 手写；动态排序必须白名单；二级缓存不用，Redis 代替」**。

================

---

## 题 7：配置、Profile 与多环境

================

## 面试问题：

`application.yml` 和 `bootstrap.yml` 区别（Boot 2 vs 3）？如何实现 dev/test/prod 多环境？敏感配置怎么管理？

## 考察点：

- `@ConfigurationProperties` 类型安全绑定
- Nacos 配置中心
- 12-factor 配置分离

## 标准答案：

**Boot 2**：`bootstrap.yml` 先加载，用于 **Spring Cloud Config / Nacos** 连接信息。  
**Boot 3 / Cloud 2020+**：bootstrap **默认不启用**，改 `spring.config.import=nacos:` 或 `spring-cloud-starter-bootstrap`。

**多环境**：

```yaml
spring:
  profiles:
    active: dev
---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/inventory_dev
---
spring:
  config:
    activate:
      on-profile: prod
```

或 **Nacos DataId**：`order-service-prod.yml`

**敏感配置**：DB 密码、JWT secret、DashScope API Key → **环境变量** / Nacos 加密 / 宝塔 **env.sh**（不进 Git）。

**@ConfigurationProperties**：`inventory.jwt.secret` 绑定到 `JwtProperties` 类，优于散落 `@Value`。

## 通俗理解：

Profile 像 **同一套系统三套开关**：开发用本地库，生产用云库；密钥从 **环境变量抽屉** 拿，不写在代码里。

## 项目结合：

- **服务器** `/opt/inventory-ms/env.sh`：`export DASHSCOPE_API_KEY=...`，Docker `env_file` 引用
- **Gitee Go CD** 部署不传密钥，服务器预置 env.sh
- **Feign URL**：dev 直连 localhost，prod 走 **Nacos 服务名** `inventory-service`
- **ai-service**：Key 未配置时 **降级固定话术**（见 AI 手册）

## 面试官追问：

1. `@Value` 和 `@ConfigurationProperties` 怎么选？
2. 配置热更新怎么做？
3. 同一配置多处引用如何防不一致？

## 高级回答：

- **ConfigurationProperties**：批量、校验 `@Validated`、IDE 提示；**@Value** 适合单值。
- **Nacos 热更新**：`@RefreshScope` Bean 或 `@ConfigurationProperties` + refresh event；**DataSource 热更** 慎用。
- **单一真相**：连接串只在 Nacos 一处，各服务 import。
- 10 年答法：**「个人项目 env.sh + profile；上云 Nacos；密钥零进仓库」** — 与 CI/CD 文档一致。

================

---

## 题 8：Spring 事件、异步与扩展点

================

## 面试问题：

`ApplicationEvent` 怎么用？`@Async` 要注意什么？你还用过哪些 Spring 扩展点（BeanPostProcessor、InitializingBean 等）？

## 考察点：

- 解耦（下单后发事件写日志、发 MQ）
- 异步线程池与事务边界
- 扩展意识（不是背 API）

## 标准答案：

**Spring 事件**：

```java
// 发布
applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderId));
// 监听
@EventListener
@Async
public void onOrderCreated(OrderCreatedEvent e) { ... }
```

- **同步默认**：监听器在 **同一事务线程**（事务未提交时监听器能读到未提交？要注意 — 一般 `@TransactionalEventListener(phase = AFTER_COMMIT)` 更安全）

**@Async**：

- 需 `@EnableAsync` + **ThreadPoolTaskExecutor** Bean
- **同类调用不异步**（代理问题）
- 异常 **不会** 传给调用方，要 try/log 或 AsyncUncaughtExceptionHandler
- 与 **SecurityContext、RequestContext** 不自动传递，要 **DelegatingSecurityContextRunnable**

**常用扩展点**：

| 扩展 | 用途 |
|------|------|
| BeanPostProcessor | 统一加工 Bean（较少手写） |
| InitializingBean | 初始化逻辑，更推荐 @PostConstruct |
| HandlerInterceptor | 登录态、日志 MDC |
| ResponseBodyAdvice | 统一响应包装 |

## 通俗理解：

事件像 **内部广播**：「订单创建了！」日志模块、统计模块各听各的，下单代码不用挨个 call。  
@Async 像 **另开工人干活**，主线程不等，但 **丢活要有日志**。

## 项目结合：

- **下单成功**：`AFTER_COMMIT` 发事件 → 写操作日志 / 发 RabbitMQ（若接入）
- **商品变更**：异步 **触发 embedding reindex**（AI 模块），不阻塞 CRUD 响应
- **线程池隔离**：AI 调用用 **独立 executor**，避免占满默认池导致下单慢
- **MDC traceId**：Gateway 生成 traceId，Feign Interceptor 传递，日志串联

## 面试官追问：

1. `@TransactionalEventListener` 和 `@EventListener` 区别？
2. 默认 @Async 用的什么线程池？
3. Spring 和 Spring Cloud 边界？

## 高级回答：

- **AFTER_COMMIT**：事务提交后才发，避免回滚了消息已出去。
- **默认 Async**：`SimpleAsyncTaskExecutor` **每任务新线程**，生产必须 **自定义池**。
- **Spring Cloud**：Nacos、OpenFeign、Gateway、LoadBalancer — **微服务治理**；Spring Boot 是 **单应用基座**。
- 10 年答法：**「解耦用事件+after commit；异步必自定义线程池+异常处理；扩展点知道 Interceptor/Advice 即可，不滥用 BeanPostProcessor」**。

================

---

## 本批小结

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| IoC/DI | 构造器注入、Bean 生命周期、循环依赖 |
| 自动配置 | Conditional、exclude、starter |
| MVC | DispatcherServlet、Filter vs Interceptor |
| AOP/事务 | 代理、自调用失效、Feign 不在同事务 |
| Security | JWT 无状态、401/403、RBAC |
| MyBatis | #{} vs ${}、一级缓存、白名单排序 |
| 配置 | profile、Nacos、env 不进 Git |
| 事件/异步 | AFTER_COMMIT、自定义线程池 |

下一批：**并发编程**（公共厕所类比）→ [面试题-06-并发编程.md](./面试题-06-并发编程.md)
