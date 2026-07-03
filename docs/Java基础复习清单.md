# Java 基础复习清单 — 检查点、面试鸭关键词与穿插计划

> **定位：** 不是重读整本 Java 书，而是补「看到并发/框架名词能立刻想到对象和线程在哪」的联想链。  
> **节奏：** 每天约 **40～60 分钟基础** + **项目/刷题仍是主线**（并发 V1～V7、门店业务等不停）。  
> **配合文档：**  
> - [`JVM与OOP-图解笔记.md`](./JVM与OOP-图解笔记.md) — OOP + 对象创建 + JVM 五区三图串联  
> - [`并发与Spring基础-面试整理.md`](./并发与Spring基础-面试整理.md)  
> - [`并发演进.md`](./并发演进.md)  
> - [`面试题-JVM分类与穿插计划.md`](./面试题-JVM分类与穿插计划.md)

---

## 一、怎么用这份清单

```text
① 按模块顺序过检查点（打勾 = 能用自己的话讲 30 秒）
② 不懂的点 → 面试鸭搜「关键词」刷 2～3 题
③ 能挂项目 → 写半句「我们 store-inventory 里…」
④ 每周五翻打勾进度，不必追求 100% 一次过完
```

**复习目标（背这个）：**

> 基础复习不是为了背语法，而是为了 **并发、Spring、Redis、MQ 不再像听外语**。

---

## 二、六块模块总览

| 块 | 名称 | 建议用时 | 和哪条主线最相关 |
|----|------|----------|------------------|
| **1** | 面向对象 | 3～4 天 | Spring Bean、Entity、接口设计 |
| **2** | 引用与对象 | 3～4 天 | HashMap、DTO、equals 陷阱 |
| **3** | 集合框架 | 4～5 天 | 业务 List/Map、ConcurrentHashMap |
| **4** | 异常与 IO | 2～3 天 | 接口报错、文件、堆外内存入门 |
| **5** | JVM 入门 | 1～2 周（穿插） | V1/V2 压测、OOM、GC |
| **6** | 多线程入门 | 2～3 周（与 V2～V7 并行） | 并发 66 题、锁、线程池 |

图例：**[并发]** **[AI]** **[微服务]** **[云原生]**

---

## 三、模块 1：面向对象

### 3.1 检查点（能讲清即打勾）

- [ ] **类 vs 对象**：类是图纸，对象是实例（一个类 many 对象）
- [ ] **封装**：private 字段 + getter/setter，隐藏内部细节
- [ ] **继承**：is-a 关系；子类 extends 父类
- [ ] **多态**：父类引用指向子类对象；运行时绑定方法（`@Override`）
- [ ] **抽象类 vs 接口**：抽象类可有实现；接口定义能力（Java 8+ 接口可有 default）
- [ ] **static**：属于类，不属于某个对象；静态方法不能直接用非静态成员
- [ ] **final**：类不可继承 / 方法不可重写 / 变量引用不可改
- [ ] **this / super**：当前对象、父类构造或方法

### 3.2 面试鸭搜索关键词

`面向对象` · `封装继承多态` · `抽象类和接口区别` · `重载和重写` · `static` · `final`

### 3.3 生活例子（30 秒）

> **类** = 学生档案模板；**对象** = 张三、李四两份档案。  
> **多态** = 「人」可以指向「学生」「员工」，叫 `work()` 时各自干各自的活。

### 3.4 项目挂钩

| 主线 | 举例 |
|------|------|
| **[并发]** | `OrderCreateConcurrencyStrategy` 接口 + V1/V2 不同实现 = **策略 + 多态** |
| **[微服务]** | 各模块 `Controller → Service → Mapper` 分层 = **封装** |

---

## 四、模块 2：引用与对象

### 4.1 检查点

- [ ] **基本类型 vs 引用类型**：栈上值 vs 栈上引用、堆上对象
- [ ] **`==` vs `equals`**：`==` 比引用；`equals` 比业务规则（默认等于 `==`）
- [ ] **为何重写 equals 要重写 hashCode**：HashMap/HashSet 先 hash 再 equals
- [ ] **String 不可变**：`final char[]` / 常量池；`StringBuilder` 拼接
- [ ] **包装类与自动装箱**：`Integer` 缓存 -128～127；`==` 对包装类陷阱
- [ ] **深拷贝 vs 浅拷贝**：对象字段仍是引用时只拷一层

### 4.2 面试鸭搜索关键词

`==和equals` · `hashCode` · `String不可变` · `Integer缓存` · `深拷贝浅拷贝`

### 4.3 生活例子

> **==** = 是不是同一张实体卡；**equals** = 学号是否相同。  
> **hashCode** = 快递柜门号；**equals** = 开柜后再核对身份证号。

### 4.4 小实验（建议手写跑一遍）

```java
// 1. == vs equals
String a = new String("abc");
String b = new String("abc");
// a==b false, a.equals(b) true

// 2. HashSet 去重
// 只重写 equals 不重写 hashCode → set.size() 可能仍是 2
```

### 4.5 项目挂钩

| 主线 | 举例 |
|------|------|
| **[并发]** | 200 线程各 `new` 请求对象 → 堆上很多实例，`==` 互不相等 |
| **[AI]** | prompt 字符串拼接用 `StringBuilder`，避免循环里 `+` |

---

## 五、模块 3：集合框架

### 5.1 检查点

- [ ] **List / Set / Map** 语义：有序可重复 / 不重复 / 键值
- [ ] **ArrayList vs LinkedList**：数组随机访问 vs 链表插删（知道即可）
- [ ] **HashMap 原理（JDK8）**：数组 + 链表 + 红黑树；hash 定位桶，equals 判等
- [ ] **HashMap 线程不安全**：并发 put 可能死循环/丢数据（了解）
- [ ] **ConcurrentHashMap**：分段/CAS+synchronized，**不能**用 `Collections.synchronizedMap` 替代高并发场景
- [ ] **HashSet 底层**：本质是 HashMap，value 是固定 PRESENT 对象
- [ ] **迭代器 fail-fast**：遍历时改结构抛 `ConcurrentModificationException`

### 5.2 面试鸭搜索关键词

`HashMap原理` · `ArrayList和LinkedList` · `ConcurrentHashMap` · `fail-fast` · `HashSet底层`

### 5.3 生活例子

> **HashMap** = 一排快递柜（数组）+ 每个柜门里链表/树挂包裹；  
> **get(key)** = 先算柜门号（hashCode），再在柜子里按 key.equals 找。

### 5.4 项目挂钩

| 主线 | 举例 |
|------|------|
| **[并发] V2** | 按 `goodsId` 存 `ReentrantLock` 的 Map ≈ **ConcurrentHashMap 思想**（分段互斥） |
| **[微服务]** | 本地缓存 Map 要考虑线程安全 vs 用 Redis |

---

## 六、模块 4：异常与 IO

### 6.1 检查点

- [ ] **Exception 体系**：Throwable → Error / Exception；Checked vs Unchecked
- [ ] **try-catch-finally**：finally 几乎总会执行（除 System.exit）
- [ ] **try-with-resources**：AutoCloseable 自动关流
- [ ] **自定义业务异常**：如 `BusinessException`，全局 `@ControllerAdvice` 捕获
- [ ] **BIO vs NIO（知道名字）**：NIO 有 Channel、Buffer、**堆外 DirectByteBuffer**
- [ ] **序列化（知道）**：JSON（项目常用）vs Java 原生序列化

### 6.2 面试鸭搜索关键词

`Checked和Unchecked异常` · `finally` · `try-with-resources` · `BIO和NIO`

### 6.3 项目挂钩

| 主线 | 举例 |
|------|------|
| **本项目** | `Result.fail("库存不足")`、全局异常处理返回统一 JSON |
| **[AI]** | 调外部 API 超时、限流 → 必须 catch 并降级 |
| **[云原生]** | 大文件上传、Netty → 堆外内存与 Direct Memory OOM |

---

## 七、模块 5：JVM 入门（详细题单见专项文档）

### 7.1 检查点（与 JVM 46 题 A+B+D+G 对齐）

- [ ] JVM 组成、内存区域（堆、栈、元空间、直接内存）
- [ ] 堆 vs 栈；对象在堆、栈帧在栈
- [ ] 类加载过程 + **双亲委派** + 如何打破（SPI、Tomcat、自定义 ClassLoader）
- [ ] GC 算法、分代、Young GC 触发
- [ ] OOM 几种；jstack / jmap / Arthas 干什么
- [ ] JIT、解释执行（执行引擎）

### 7.2 文档跳转

**按类刷题、打勾：** [`面试题-JVM分类与穿插计划.md`](./面试题-JVM分类与穿插计划.md)

### 7.3 项目挂钩

| 主线 | 举例 |
|------|------|
| **[并发] V1/V2** | 200 线程 → 200 栈；压测后 jstack 看 BLOCKED |
| **[微服务]** | `-Xms/-Xmx`、容器 memory limit |
| **[云原生]** | G1/ZGC、AOT（GraalVM） |

---

## 八、模块 6：多线程入门（与并发 66 题 + V2～V7 并行）

### 8.1 检查点

- [ ] **进程 vs 线程**；线程共享堆、栈各自独立
- [ ] **创建线程**：Thread、Runnable、Callable、线程池（**项目只用池**）
- [ ] **线程状态**：NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED
- [ ] **sleep vs wait**：sleep 不释放锁；wait 释放锁（在 synchronized 内）
- [ ] **synchronized**：对象锁、方法锁；可重入
- [ ] **volatile**：可见性 + 禁止指令重排；**不保证**复合操作原子性
- [ ] **ReentrantLock vs synchronized**：可中断、公平锁、tryLock（**V2 用的这个**）
- [ ] **线程池参数**：core、max、queue、拒绝策略
- [ ] **CountDownLatch / CyclicBarrier**（V1 压测 startGate/doneGate）
- [ ] **ThreadLocal**：线程私有；用完 **remove**（RBAC 用户上下文）

### 8.2 面试鸭搜索关键词

`创建线程的方式` · `sleep和wait` · `synchronized原理` · `volatile` · `线程池参数` · `CountDownLatch` · `ThreadLocal` · `死锁`

### 8.3 文档跳转

- 生活例子版：[`并发与Spring基础-面试整理.md`](./并发与Spring基础-面试整理.md)  
- 压测数据：[`并发演进.md`](./并发演进.md)

### 8.4 项目挂钩

| 主线 | 举例 |
|------|------|
| **[并发] V1** | 无锁 → 109/200 超锁 |
| **[并发] V2** | `ReentrantLock(goodsId)` → 100/100，响应变慢 |
| **[并发] V4** | SQL `where stock>=` → 数据库层原子性 |
| **[微服务]** | 分布式锁 Redis；**不是** JVM 锁 |

---

## 九、四条主线 × 基础模块 对照表

| 主线 | 必须先稳的基础块 | 说明 |
|------|------------------|------|
| **并发改造 V1～V7** | **2 → 6 → 5** | 引用、线程、JVM；Spring 单例见模块 1 |
| **AI 接入** | **2 → 4** | 字符串、异常、超时；大对象注意堆 |
| **微服务** | **1 → 3 → 5** | 分层 OOP、接口、JVM 参数、多实例 |
| **云原生** | **4 → 5** | NIO/堆外、容器内存、G1/ZGC/AOT |

---

## 十、推荐复习顺序（6～8 周穿插，可循环）

```text
第 1 周：模块 1（OOP）+ 模块 2 前半（== / equals / hashCode）  ← 你已从 equals 开始
第 2 周：模块 2 后半（String）+ 模块 3（HashMap / List）
第 3 周：模块 4（异常）+ 模块 5 的 A 类 JVM 题（见 JVM 文档）
第 4 周起：模块 6 与 V3/V4 代码并行 + 并发 66 题每天 3～5
第 6 周起：模块 5 E 类（G1/CMS）与微服务预习穿插
```

**不必等「基础全过完」再写 V3/V4**——写到哪、缺哪、回哪块打勾即可。

---

## 十一、第一周每日建议（可直接执行）

| 天 | 基础（40 min） | 项目 / 题 |
|----|----------------|-----------|
| **D1** | 模块 2：`==` / `equals` / `hashCode`，跑 Student demo | JVM 文档：题 3、4、9 |
| **D2** | 模块 2：String 不可变、StringBuilder | V2 口述 1 分钟 |
| **D3** | 模块 3：HashMap 放取过程（hash → equals） | 并发题：synchronized vs Lock |
| **D4** | 模块 1：多态 + 接口（对照 Strategy 策略类） | 翻 `并发与Spring基础` 第一、五节 |
| **D5** | 模块 5：双亲委派 + 打破（SPI/Tomcat） | JVM 题：32；并发题 2 题 |
| **D6** | 模块 6：线程创建 + 线程池参数 | V1 测试类：startGate/doneGate |
| **D7** | 复盘本周打勾项；不会的汇总问我 | 休息或轻量刷题 |

---

## 十二、总进度打勾（模块级）

```text
[ ]  模块 1  面向对象
[ ]  模块 2  引用与对象
[ ]  模块 3  集合框架
[ ]  模块 4  异常与 IO
[ ]  模块 5  JVM 入门（细项见 JVM 文档 46 题卡）
[ ]  模块 6  多线程入门（细项见并发 66 题 + 并发演进）
```

---

## 十三、口述模板（基础串并发，面试 1 分钟）

> Java 里引用类型变量存的是地址，`==` 比是不是同一对象；业务相等要重写 `equals`，用 HashMap 还要配 `hashCode`。  
> 类由类加载器加载，双亲委派先让父加载器加载核心类。对象在堆上，线程各有栈；Spring Service 单例无状态，多线程调同一个实例没问题，但共享的可变数据或数据库读写仍要并发控制。  
> 我们项目 V1 无锁 200 并发超锁，V2 用 ReentrantLock 按商品互斥解决，根因是 DB 竞态不是单例。

---

*遇到不懂：发「模块几 + 检查点 + 你的理解」，可按生活例子 + 项目 + 面试话术讲解。同一检查点隔几天再问一遍完全正常。*
