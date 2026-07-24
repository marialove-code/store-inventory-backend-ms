# 中高级 Java 后端面试题库 · 总索引

> **定位**：北京互联网 / 金融科技公司 · Java 后端中高级岗位  
> **面向背景**：10 年经验 · 企业级 CRUD · 项目组长 · 理论基础一般 · 自研《门店进销存管理系统》恢复手感  
> **原则**：真实面试场景，非培训机构纯八股；每题含原理、场景、坑、方案、项目结合、追问与 10 年深度答法  

---

## 候选人项目锚点（答题时统一引用）

| 项 | 内容 |
|----|------|
| 架构 | 原单体 → 微服务改造（订单、库存等） |
| 中间件 | Nacos、Gateway、OpenFeign、Sentinel、Redis |
| 存储 | MySQL / PostgreSQL（含 pgvector） |
| 安全 | Spring Security + JWT |
| AI | Spring AI（客服、智搜、补货预警等） |
| 业务 | 商品、库存、订单、RBAC、操作日志 |

---

## 分批生成进度

| 批次 | 模块 | 文档 | 状态 |
|------|------|------|------|
| 第 1 批 | Java 基础 | [面试题-01-Java基础.md](./面试题-01-Java基础.md) | ✅ 已完成（8 题） |
| 第 2 批 | JVM | [面试题-02-JVM.md](./面试题-02-JVM.md) | ✅ 已完成（8 题） |
| 第 3 批 | 网络 | [面试题-03-网络.md](./面试题-03-网络.md) | ✅ 已完成（8 题） |
| 第 4 批 | MySQL/PostgreSQL | [面试题-04-MySQL与PostgreSQL.md](./面试题-04-MySQL与PostgreSQL.md) | ✅ 已完成（8 题） |
| 第 5 批 | Spring 全家桶 | [面试题-05-Spring全家桶.md](./面试题-05-Spring全家桶.md) | ✅ 已完成（8 题） |
| 第 6 批 | 并发编程 | [面试题-06-并发编程.md](./面试题-06-并发编程.md) | ✅ 已完成（8 题，公共厕所类比） |
| 第 7 批 | Redis | [面试题-07-Redis.md](./面试题-07-Redis.md) | ✅ 已完成（8 题） |
| 第 8 批 | Elasticsearch | [面试题-08-Elasticsearch.md](./面试题-08-Elasticsearch.md) | ✅ 已完成（8 题） |
| 第 9 批 | 微服务架构 | [面试题-09-微服务架构.md](./面试题-09-微服务架构.md) | ✅ 已完成（8 题，含 Eureka vs Nacos） |
| 第 10 批 | 云原生 Docker/K8s | [面试题-10-云原生Docker与Kubernetes.md](./面试题-10-云原生Docker与Kubernetes.md) | ✅ 已完成（8 题） |

**全部 10 批已完成。**

---

## 单题标准格式

每道题均包含：

1. **面试问题**
2. **考察点**
3. **标准答案**
4. **通俗理解**
5. **项目结合**（进销存）
6. **面试官追问**
7. **高级回答**（10 年开发者深度）

---

## 与其他面试文档的关系

| 文档 | 关系 |
|------|------|
| [AI功能复习手册.md](./AI功能复习手册.md) | AI 模块口述 |
| [LangChain与SpringAI对照-复习手册.md](./LangChain与SpringAI对照-复习手册.md) | AI 概念对照 |
| [面试题-SpringCloudAlibaba.md](./面试题-SpringCloudAlibaba.md) | 微服务专题（本库第 9 批会互补） |
| [面试题-JVM分类与穿插计划.md](./面试题-JVM分类与穿插计划.md) | JVM 46 题穿插计划（本库第 2 批会互补） |
| [并发/00-总览-V1到V7.md](./并发/00-总览-V1到V7.md) | 并发演进与压测（本库第 6 批会互补） |

**复习建议**：本库按模块系统刷；专文（SCA、Sentinel、RBAC）按投递岗位加练。

---

## 本批自测清单（Java 基础）

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| HashMap | 结构、扩容、树化、非线程安全 |
| List 选型 | ArrayList 默认首选、迭代删除坑 |
| equals/hashCode | 契约、BigDecimal、业务键 |
| String | 不可变、Builder 拼接 |
| 异常 | BizException + Advice、错误码 |
| 泛型 | 擦除、PECS、PageResult |
| Stream | 适用边界、parallel 慎用 |
| 接口 | Feign/Service 分层、default 方法 |

## 本批自测清单（JVM）

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| 内存结构 | 堆/栈/元空间、对象在堆引用在栈 |
| 类加载 | 双亲委派、SPI 打破 |
| GC 分代 | Minor vs Full、复制 vs 整理 |
| G1 | Region、Mixed GC、JDK 默认 |
| OOM | 类型、dump、MAT、OOMKilled |
| JIT | 热点、先 profiling 再优化 |
| 引用 | 生产用 Redis/Caffeine 非 SoftReference |
| 容器 | MaxRAMPercentage、非堆余量 |

## 本批自测清单（网络）

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| TCP | 三次握手/四次挥手、TIME_WAIT、连接池 |
| HTTP/REST | 幂等、状态码、进销存 API 风格 |
| HTTPS | TLS 作用、Nginx 终结 SSL |
| DNS/LB | L4/L7、Gateway vs Nacos |
| 超时重试 | connect/read、非幂等慎重试 |
| Keep-Alive | 连接池、Feign HttpClient |
| 粘包/NIO | HTTP 定界、Gateway Netty |
| Nginx | 502/504 排查、Forwarded 头 |

## 本批自测清单（MySQL/PostgreSQL）

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

## 本批自测清单（Spring 全家桶）

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

## 本批自测清单（并发 · 公共厕所）

| 版本/主题 | 要能脱口而出的点 |
|-----------|-----------------|
| V1 竞态 | 先查再写超锁 109/100 |
| V2 JVM锁 | 单机串行、多实例不够 |
| volatile | 可见非原子，库存不靠它 |
| V3 线程池 | 旁路不解锁瓶颈 |
| V4 SQL | 条件 UPDATE 防超卖底座 |
| V5 Redis锁 | NX+Lua、看门狗、慢于V4 |
| V6 MQ | 秒回、最终一致、消费者V4 |
| V7 幂等 | 防连点、补偿放坑 |

## 本批自测清单（Redis）

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| 数据类型 | String 锁/计数，Hash 多设备 |
| 三大问题 | 穿透空值、击穿互斥、雪崩抖动 |
| JWT+Redis | 双校验、登出删 key |
| 分布式锁 | NX+Lua，库存靠 V4 不靠锁 |
| 幂等限流 | SETNX、INCR 固定窗口 |
| 持久化 | RDB/AOF，宕机登录态丢 |
| 高可用 | 主从哨兵 Cluster 场景 |
| Spring | Lettuce、前缀、TTL |

## 本批自测清单（Elasticsearch）

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

## 本批自测清单（微服务 · 含 Eureka）

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| 何时拆 | 痛点驱动，集群≠微服务 |
| 注册中心 | 注册/发现/调用三角 |
| Eureka vs Nacos | AP、自我保护、配置一体、金风 vs 进销存 |
| Gateway/Feign | 外网 Gateway，内部 Feign |
| 拆分边界 | 库存写主权在 inventory |
| Sentinel | 与 Nacos 正交 |
| 分布式事务 | V4+幂等+补偿 |
| 部署 | order Docker CD、traceId |

## 本批自测清单（云原生）

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| Docker 三宝 | 镜像/容器/Dockerfile |
| Dockerfile | JRE、多阶段、密钥不进镜像 |
| 网络 | host 连宿主机 Nacos/PG |
| CI/CD | Gitee push CI、手动 CD |
| 资源 | MaxRAMPercentage、OOMKilled |
| K8s 口述 | Pod/Deployment/Service |
| 配置 | env.sh、12-Factor |
| 全链路 | nestparts.top → order 容器 |
