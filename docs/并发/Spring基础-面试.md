# 并发与 Spring 基础 — 面试整理（生活例子版）

> 整理自 V1 超锁压测与 Spring/并发基础问答。  
> 结合本项目：`OrderCreateConcurrencyV1Test`、RBAC、`OrderInfoServiceImpl`。

---

## 一、V1 超锁 / 超卖（本项目语境）

### 1.1 叫什么？

| 说法 | 含义 | V1 结果 |
|------|------|---------|
| **超锁** | `lockStock > stock`，锁定超过可售 | lockStock=109，stock=100 |
| **超卖** | 成交/卖出超过真实库存 | 口语常混用；本项目阶段更准确是 **超锁（预占超额）** |

### 1.2 根因（面试 30 秒）

`createOrder` 流程：**读可用库存 → 判断 → 写订单 → lockStock**，中间无互斥。  
200 线程同时读到 `usableStock ≥ 1`，都通过校验 → 成功 109 笔，lockStock=109。

**生活例子：** 仓库账本显示还剩 1 件，10 个人 **同时** 看账本，都以为能买，都下单 —— 不是收银员（Service）只有一个人有问题，是 **账本（数据库）更新没加锁**。

### 1.3 和 Spring 单例的关系

- Service 是单例 → 200 线程调 **同一个** `OrderInfoService` → **正常**（Service 无状态）
- 超锁 **不是** 单例导致的，是 **DB 层竞态**

---

## 二、JUnit 并发压测（CountDownLatch + 线程池）

### 2.1 在测什么？

- 200 个线程 **尽量同时** 调用 `createOrder("v1", ...)`
- 统计成功/失败，查库看 lockStock 是否 > 100

### 2.2 主线程 vs 子线程

**生活例子：公司只有 1 个 HR（主线程），200 个工人（线程池）**

| 谁 | 干什么 |
|----|--------|
| **主线程 main** | for 循环 **派 200 个任务** → 发令 → 等全部完成 → 查库打日志 |
| **子线程 pool-x** | 在起跑线等 → 发令后 **真正下单** |

**易错点：** for 里只有 `submit` 是 main 执行的；`await()`、`createOrder()` 在 **子线程** 里，不能按文件从上往下当成一条线。

### 2.3 startGate（发令枪）

```java
CountDownLatch startGate = new CountDownLatch(1);  // 初始 1
// 子线程：startGate.await()  →  阻塞
// 主线程：startGate.countDown() → 1 变 0，所有子线程一起跑
```

**生活例子：** 200 名运动员到起跑线等着；裁判鸣枪（countDown）后一起开跑。  
**没有 countDown：** 运动员永远等，主线程最后在 doneGate 超时失败。

### 2.4 doneGate（签到表）

```java
CountDownLatch doneGate = new CountDownLatch(200);  // 初始 200
// 每个子线程 finally：doneGate.countDown()
// 主线程：doneGate.await() 等 200 次签到完再查库
```

**生活例子：** 终点 200 个打卡位，裁判等所有人都到才公布成绩。

### 2.5 为什么要两个 Latch？

| | startGate | doneGate |
|---|-----------|----------|
| 解决 | **何时一起开始** | **何时全部结束** |
| 初始计数 | 1 | 200 |
| 谁 countDown | 主线程 1 次 | 每个子线程 1 次 |
| 谁 await | 子线程 | 主线程 |

### 2.6 并发数与「同时性」

- 200 并发 + 200 线程池：起跑 **较齐**，V1 结果稳定 109
- 数量很大时：先 submit 完的先跑，后到的后跑 → **齐射变波浪**（模拟误差变大，不是串行排队）
- 要更齐：可加 **readyGate**（所有人到 await 后再发令）

---

## 三、线程相关面试点（结合本项目）

| 知识点 | 生活/项目 |
|--------|-----------|
| 线程池 | 固定 200 工人，复用线程，不每次 new Thread |
| AtomicInteger | 多人同时计数用 **原子计数器**（成功/失败统计）；**不能**解决业务超锁 |
| 竞态条件 | 多人同时改同一本账，无锁 → 超锁 |
| 原子性/可见性/有序性 | 读-判-写 非原子；volatile 不解决 i++ 和整段业务 |

---

## 四、Spring 为什么是单例？

### 4.1 IoC 容器（生活：公司 HR + 仓库）

- **IoC** = 控制反转：谁 `new`、谁组装依赖 → 交给 **Spring 容器**
- **容器** = 创建 Bean、存进仓库、谁 `@Autowired` 就派发引用

**不是你写：** `new OrderInfoService()`  
**而是：** 容器启动时创建，注入给 Controller、测试类等。

### 4.2 单例在 Spring 里指什么？

**每种 Bean 在容器里通常只有 1 个实例**，所有注入点 **共用同一引用**。

**生活例子：** 全公司 **会计岗位只招 1 人**，订单部、采购部找会计都是 **同一个小张**。

**不是：** 整个 Spring 只有一个对象（会计 1 个、保安 1 个、店长 1 个 —— 各岗位各 1 份）。

### 4.3 为什么默认单例？

1. **省开销**：创建一次，全程复用，少 GC  
2. **Service 无状态**：像工具人，适合共用  
3. **好做 AOP/事务**：代理包一层，全应用统一  
4. **统一管理生命周期**：启动创建，关闭销毁  

### 4.4 生命周期（简）

```
容器启动 → 创建单例 Bean → 注入依赖 → 运行期复用 → 容器关闭 → 销毁
```

`@Lazy`：第一次用到才创建，但创建后 **仍是 1 个**。  
`prototype`：每次获取 **new 新的**。

---

## 五、无状态（Stateless）

### 5.1 一句话

**不在单例实例的成员变量里，保存「当前这次请求 / 当前用户 / 当前订单」等会变的数据。**

**生活例子：** 收银员小张全店只有 1 人，**工牌后面不贴「正在服务：张三」**；谁来了 **递单子（参数）**，办完就忘。

### 5.2 200 线程为什么会「乱套」？（有状态时）

```java
// ❌ 错误：成员变量 = 全店共用的小本本
private String currentUser;

public void createOrder(OrderInfoDTO dto) {
    this.currentUser = dto.getUserName();  // 线程 A 写张三
    // ... 中间线程 B 改成李四 ...
    saveOrder(currentUser);                // A 可能存成李四
}
```

**生活例子：** 200 个顾客 **共改小本本同一行**，后写的覆盖先写的。

### 5.3 无状态时数据放哪？

| 方式 | 生活 | 项目 |
|------|------|------|
| **参数** | 顾客递来的单子 | `OrderInfoDTO dto` |
| **局部变量** | 手里便签，用完就扔 | `OrderInfo order = new ...` |
| **数据库** | 公司正式台账 | 库存、订单表 |
| **ThreadLocal** | 每个窗口临时胸牌，下班摘掉 | `LoginUserContext` + **finally clear** |

### 5.4 @Autowired / final 也是成员变量，算无状态吗？

**算。** 它们是 **工具（扫码枪、Mapper）**，注入后 **引用不变**，不是「当前顾客是谁」。

```java
private final InventoryStockMapper inventoryStockMapper;  // ✅ 工具
private Long currentUserId;                               // ❌ 请求状态
```

**记法：成员变量分两种 —— 工具 vs 小本本；无状态 = 可以有工具，不能有小本本。**

---

## 六、RBAC 里的「无状态」（Session STATELESS）

### 6.1 两层不要混

| 层 | 含义 |
|----|------|
| **Bean 无状态** | Service 不存 currentUser 成员变量 |
| **Session 无状态** | `SessionCreationPolicy.STATELESS`，不用 HttpSession 存登录 |

### 6.2 本项目链路

```
JWT + Redis 存登录态
  → Filter 里 LoginUserContext.setUser()（ThreadLocal）
  → 业务
  → finally LoginUserContext.clear()  // 防线程池串用户
```

**生活例子：** 银行每窗口 **胸牌** 写「正在办谁」；办完 **必须摘掉**，否则下一位客户还挂着上一人的牌。

**JWT 无状态 + Redis 有状态登录态：** Token 带身份，Redis 控制还能不能用、能否踢人。

---

## 七、饿汉式 / 懒汉式 / @Lazy / prototype

| 概念 | 何时创建 | 几个实例 | 生活 |
|------|----------|----------|------|
| **Spring 默认 singleton** | 启动时 | 1 | 开业前招好会计 |
| **@Lazy singleton** | 第一次用 | 1 | 坏了才叫维修师傅 |
| **prototype** | 每次获取 | 多个 | 每单雇临时工 |
| **设计模式 饿汉式** | 类加载 | 1 | 类一加载就有 INSTANCE |
| **设计模式 懒汉式** | 第一次 getInstance | 1 | 第一次才 new |

**@Lazy ≠ prototype：** Lazy 只是 **晚一点创建**，创建后仍 **共用 1 个**。

---

## 八、构造方法与单例模式

### 8.1 构造方法是干什么的？

**`new 类名()` 就是在调构造方法 → 创建对象。**

### 8.2 默认构造（不写任何构造时）

编译器自动加：`public 类名() { }`  
**生活：** 没写开业规矩 → 默认 **大门敞开，空手开一家空店**（不是默认帮你定经营内容，字段仍是 null/0）。

**一旦写了任意构造 → 默认无参构造不再自动提供。**

### 8.3 private Singleton() {} 干什么？

**把门从外面锁上，外面不能 `new`，只能类内部或 getInstance 控制唯一实例。**

```java
private static final Singleton INSTANCE = new Singleton();  // 内部任命唯一
private Singleton() {}   // 禁止外面再封总经理
public static Singleton getInstance() { return INSTANCE; }
```

**不写 private 构造：** 编译器给 public 默认构造 → 外面也能 `new` → **单例破灭**。

### 8.4 Spring 为什么 Service 不用 private 构造？

容器（HR）要在 **类外部** 反射创建 Bean → 一般 **public 构造**。  
单例靠 **容器只创建 1 份**，不是靠 private 构造。

| | GoF 手写单例 | Spring Bean |
|---|--------------|---------------|
| 谁创建 | 类自己 getInstance | IoC 容器 |
| 构造 | 通常 private | 通常 public |
| 如何保证 1 个 | 禁止外部 new | 容器只注册 1 个 |

---

## 九、类 vs 对象（学生店比喻）

| 概念 | 生活 |
|------|------|
| **类** | 「学生店」加盟手册 / 图纸 |
| **对象** | 实际开的那一家门店 |
| **new** | 新开一家店 |
| **默认构造** | 允许零门槛开 **空铺** |
| **单例** | 这种岗位全公司 **只开 1 家** |

---

## 十、面试口述模板（串联项目）

> 我们做了库存并发演进。V1 无并发控制，JUnit 200 线程 + CountDownLatch 统一起跑，稳定复现超锁：成功 109，lockStock=109，stock=100。根因是 createOrder 读库存和 lockStock 非原子，不是 Spring 单例问题。  
>  
> Spring 默认 singleton，IoC 容器启动创建 Bean，无状态 Service 多线程共用；RBAC 用 STATELESS Session + JWT + Redis，当前用户放 ThreadLocal 并在 Filter finally 里 clear。  
>  
> V2 计划用 JUC 锁解决超锁，同样 200 并发对比指标。

---

## 十一、今日 V1 压测数据（基线）

| 轮次 | 成功 | 失败 | lockStock | usable | 超锁 |
|------|------|------|-----------|--------|------|
| 第 1～3 次 | 109 | 91 | 109 | -9 | 是（三轮一致） |

接口：`POST /api/order/concurrency/order/add?version=v1`  
详见：`docs/并发/01-压测数据.md`

---

## 十二、后续计划

1. **JMeter**：同一接口 HTTP 压测 1～2 次佐证  
2. **V2**：JUC 锁 / synchronized，目标成功 ≤ 100，不超锁  

---

*文档随并发改造持续更新。*
