# 第 2 批：JVM 面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 互补：[面试题-JVM分类与穿插计划.md](./面试题-JVM分类与穿插计划.md) · [JVM与OOP-图解笔记.md](./JVM与OOP-图解笔记.md)  
> 本批共 **8 题** · 下一批：网络

---

## 题 1：JVM 内存结构与线程栈

================

## 面试问题：

请画出（或口述）JVM 运行时数据区，说明堆、栈、方法区（元空间）各自存什么？一个线程访问方法时栈里有什么？

## 考察点：

- 线程私有 vs 线程共享区域
- 栈帧、局部变量表、操作数栈（能说到局部变量即可）
- JDK 8 方法区 → Metaspace 的变化
- 能否区分「对象在堆、引用在栈」

## 标准答案：

**线程共享：**

| 区域 | 存什么 | 异常 |
|------|--------|------|
| **堆 Heap** | 对象实例、数组 | OOM: Java heap space |
| **元空间 Metaspace** | 类元信息（类名、方法、字段、常量池结构等） | OOM: Metaspace（本地内存） |
| **直接内存** | NIO DirectBuffer、部分 Netty 缓冲 | OOM: Direct buffer memory |

**线程私有：**

| 区域 | 存什么 | 异常 |
|------|--------|------|
| **虚拟机栈** | 栈帧：局部变量表、操作数栈、动态链接、方法出口 | StackOverflowError / 线程栈 OOM |
| **程序计数器 PC** | 当前字节码行号 | 几乎不会 OOM |
| **本地方法栈** | Native 方法 | 同栈 |

**一次方法调用**：压入栈帧；局部变量表放 **基本类型值** 和 **对象引用**（引用指向堆中对象）；返回时弹栈。

**JDK 8 变化**：永久代 PermGen 移除，类元数据进 **Metaspace**（本地内存，默认受 `-XX:MaxMetaspaceSize` 限制）；字符串常量池在 **堆**。

## 通俗理解：

JVM 像一家 **进销存公司大楼**：

- **堆** = 大仓库，所有「货」（对象）堆在这里，所有线程共享访问。
- **栈** = 每个员工（线程）自己的 **工作台**，上面放当前正在处理的单据编号（引用）和临时算数结果；换任务就换一层台面（栈帧）。
- **元空间** = **档案室**，存放商品分类手册、流程 SOP（类定义），不是每件货本身。

## 项目结合：

- **order-service** 一次下单：`OrderController` 栈帧里局部变量有 `CreateOrderDTO` 引用，真正 DTO 在堆；Feign 调 inventory 再压新栈帧。
- **JWT 解析** 后 `LoginUser` 对象在堆；SecurityContext 里 ThreadLocal 存的是 **引用**，线程结束栈回收，但堆对象要等 GC。
- 单体改微服务后 **JVM 进程变多**：原来 1 个 4G 堆，现在 5 个服务各 512M～1G，要算 **总内存**，不是单服务看起来更小就安全。

## 面试官追问：

1. 成员变量、静态变量、局部变量分别在哪？
2. 对象一定在堆上吗？（逃逸分析、栈上分配、TLAB 加分项）
3. `-Xms` 和 `-Xmx` 为什么要设成一样？

## 高级回答：

- **成员变量** 随对象在堆；**static** 在方法区/元空间（引用指向堆对象）；**局部变量** 在栈帧局部变量表。
- **逃逸分析**：JIT 发现对象未逃出方法，可能 **标量替换** 不进堆；面试说「大部分 new 在堆，JIT 会优化热点」即可。
- **Xms=Xmx**：避免运行时动态扩堆触发 Full GC；容器里配合 `-XX:MaxRAMPercentage` 更常见。
- 进销存 **批量导出** 若一次 `List<>` 装 10 万行在堆，要分页流式，否则 young gc 频繁 + 可能 heap OOM。

================

---

## 题 2：类加载过程与双亲委派

================

## 面试问题：

类加载分哪几个阶段？什么是双亲委派？为什么要双亲委派？如何打破（举一例）？

## 考察点：

- loading → linking（verify/prepare/resolve）→ initialization
- Bootstrap / Extension / Application ClassLoader 层次
- 安全与重复加载防护
- SPI、Tomcat、Spring Boot fat jar 的实际场景

## 标准答案：

**阶段：**

1. **Loading**：读字节流，生成 `Class` 对象，方法区存元数据
2. **Linking**：验证、准备（静态变量默认零值）、解析（符号引用→直接引用，可延迟）
3. **Initialization**：执行 `<clinit>`，静态块、静态赋值

**双亲委派**：类加载请求先交给 **父加载器**；父无法加载才自己 load。  
**目的**：

- 避免 **重复加载** 核心类
- **安全**：防止用户自定义 `java.lang.String` 替换 JDK 类

**打破例子**：

- **SPI**：JDBC `DriverManager` 由 Bootstrap 加载，但实现 jar 在 classpath，需 **线程上下文类加载器 TCCL**
- **Tomcat**：WebAppClassLoader 先加载 WEB-INF/classes 再委派（隔离多应用）
- **Spring Boot Loader**：LaunchedURLClassLoader 加载 BOOT-INF/lib

## 通俗理解：

双亲委派像 **总部采购制度**：门店要买「标准包装箱」（JDK 类）必须先问总部有没有统一供应商，不能自己随便印 logo 的箱子冒充标准箱。

## 项目结合：

- **Spring Boot 可执行 jar**：`java -jar order-service.jar` 用 **JarLauncher**，自定义类加载读嵌套 lib，面试能提一句即可。
- **Nacos / Sentinel client** 通过 SPI 加载实现，和 JDBC Driver 同类问题。
- 自己写 **自定义 ClassLoader** 在进销存里几乎不需要；知道 **不要用自定义类加载器玩热部署** 除非有运维诉求。

## 面试官追问：

1. 静态变量什么时候赋值？实例变量呢？
2. 两个 ClassLoader 加载同一 class 文件，Class 对象相等吗？
3. `<clinit>` 和 `<init>` 区别？

## 高级回答：

- **静态变量**：prepare 零值 → initialize 赋真实值；**实例变量** 在 new 时 `<init>` 构造。
- 不同 ClassLoader → **不同 Class 对象**，强转抛 `ClassCastException`（Tomcat 跨应用常见问题）。
- 进销存 **@Configuration 静态@Bean** 在类初始化时触发，注意静态块里别调还在加载中的类造成 **Circular initialization**。

================

---

## 题 3：垃圾回收算法与分代模型

================

## 面试问题：

常见 GC 算法有哪些？为什么 HotSpot 用分代收集？Minor GC 和 Full GC 一般回收哪些区域？

## 考察点：

- 标记-清除、复制、标记-整理
- 新生代 Eden/S0/S1、老年代
- GC 触发直觉（Eden 满、老年代满、Metaspace 满、System.gc）
- 不是背收集器参数，而是理解 trade-off

## 标准答案：

**算法：**

| 算法 | 思路 | 缺点 |
|------|------|------|
| 标记-清除 | 标记存活 → 清掉垃圾 | 碎片 |
| 复制 | 存活复制到另一块，清空原区 | 浪费一半空间（新生代用） |
| 标记-整理 | 标记 → 存活向一端移动 | 移动成本高（老年代用） |

**分代假设**：

- 大部分对象 **朝生夕死**（订单 DTO、临时 List）
- 少数对象 **长期存活**（缓存 Singleton、连接池）

**HotSpot 分代**：

- **新生代**：Eden + Survivor(from/to)，**Minor GC**，复制算法
- **老年代**：存活多次的对象晋升，**Major/Full GC** 常含老年代，标记-整理或整体收集

**Minor GC**：通常 **新生代**；**Full GC** 往往 **整堆 + 方法区/元空间**（视收集器与触发原因而定），STW 更长，要尽量避免频繁 Full GC。

**对象晋升**：年龄达阈值（默认 15）或 Survivor 放不下大对象可能 **直接进老年代**。

## 通俗理解：

GC 像 **仓库定期清仓**：

- **Minor GC** = 清 **临时周转区**（今日下单暂存区），大部分包装箱直接扔，少量留到「常备货架」（Survivor）。
- **Full GC** = **全仓大盘点**，要停业一会儿（STW），员工都停下来等清点完。

## 项目结合：

- 进销存 **高峰下单**：短生命周期对象多（Request、VO、MyBatis 中间 List），Young GC 频繁 **正常**；要警惕的是 **老年代持续上涨**（内存泄漏或缓存无上限）。
- **AI 模块** 一次 RAG 请求拼大 String、List chunk，请求结束应可回收；若把 embedding 全放 **静态 Map** 不淘汰 → 老年代涨 → Full GC 变长。
- **Redis 做缓存、JVM 堆只做请求级对象** 是正确分工，堆里没有「全量 SKU 缓存」。

## 面试官追问：

1. 什么是 STW？能完全消除吗？
2. 安全点 Safepoint 是什么？
3. 如何判断频繁 Full GC 是泄漏还是堆太小？

## 高级回答：

- **STW**：Stop-The-World，GC 线程工作时业务线程暂停；低延迟收集器（ZGC、G1 目标）缩短 STW，不是零 STW。
- **Safepoint**：线程只在安全点才能开始 GC；长时间 counted loop 可能延迟进入 Safepoint（少见坑）。
- **判断**：`jstat GC` 看 OU（老年代）是否 **只升不降**；heap dump 看 Dominator Tree；压测后老年代回收后基线是否越来越高。
- 进销存 **order 容器 512M**：Young GC 50ms 内可接受；Full GC 200ms+ 会影响 P99，要调堆或查泄漏。

================

---

## 题 4：G1 收集器与生产选型

================

## 面试问题：

G1 的特点是什么？和 CMS 比有什么改进？JDK 11+ 默认收集器是什么？你的服务会怎么选 GC？

## 考察点：

- Region 化、可预测停顿（MaxGCPauseMillis）
- Remember Set、Mixed GC
- CMS 已 deprecated 的背景
- 结合微服务小堆的务实选型

## 标准答案：

**G1（Garbage First）**：

- 堆划 **等大小 Region**（不必物理连续分代）
- 跟踪每个 Region 垃圾占比，优先回收 **垃圾最多** 的 Region
- 目标停顿：`-XX:MaxGCPauseMillis=200`（期望非保证）
- **Mixed GC**：回收部分老年代 Region
- 避免 CMS 的 **碎片** 和 **Concurrent Mode Failure** 雪崩（CMS 已逐步淘汰）

**JDK 9+ 默认 G1**；JDK 17/21 服务端仍是 G1 为主；**ZGC/Shenandoah** 适合 **超大堆、极低延迟**（毫秒级 STW），JDK 17+ ZGC 生产可用度提高。

**进销存类微服务（ pragmatic ）**：

| 场景 | 建议 |
|------|------|
| 单服务堆 512M～2G，JDK 17 | **G1 默认** + 设 `-Xms=-Xmx` |
| 容器 512M 限制 | `-XX:MaxRAMPercentage=75.0`，留内存给 Metaspace、线程栈、Direct |
| 极低延迟支付级 | 才考虑 ZGC + 更大内存 |

## 通俗理解：

G1 像 **按街区扫垃圾**：哪个街区垃圾多先扫哪个，争取每次只停 200ms，而不是等整个城市臭了再全城大扫除（Full GC）。

## 项目结合：

- **宝塔 Docker 跑 order-service**：容器 memory limit 1G，JVM 不要 `-Xmx1024m`（会 OOMKilled），应 **MaxRAMPercentage** 或 `-Xmx768m` 留余量。
- **Gateway** 流量入口、对象短命，G1 合适；**ai-service** 若 embedding 批处理瞬时占堆，看 Mixed GC 频率调 `-XX:InitiatingHeapOccupancyPercent`。
- 面试诚实说：**个人项目未做 GC 日志长期分析**，但知道 **上线看 GC log / Prometheus jvm_memory** 是标配。

## 面试官追问：

1. CMS 的 Concurrent Mode Failure 是什么？
2. G1 的 Humongous 对象是什么？
3. 如何开启 GC 日志？JDK 11 用什么参数？

## 高级回答：

- **CMF**：CMS 并发清理跟不上分配，退化为 Serial Old Full GC，停顿暴增。
- **Humongous**：超过 Region 一半的大对象进特殊 Region，可能触发 Full GC；进销存 **大 byte[] 导出** 要注意。
- **GC 日志**：JDK 11+ `-Xlog:gc*:file=gc.log:time,uptime,level,tags`；配合 **GCEasy / GCViewer** 分析。
- 10 年答法：不说「调过 50 次 GC 参数」，说 **「先定位是泄漏还是容量，再调堆；收集器用默认 G1，除非有 SLA 证据换 ZGC」**。

================

---

## 题 5：OOM 类型与排查思路

================

## 面试问题：

Java 常见 OOM 有哪几种？线上接口突然 502/进程消失，你如何排查是否 JVM OOM？步骤是什么？

## 考察点：

- heap / metaspace / stack / direct / unable to create native thread
- OOMKilled vs Java OOM
- jmap、jstat、heap dump、MAT 思路
- 能否讲一个「完整排查故事」（即使来自个人项目压测）

## 标准答案：

**常见 OOM：**

| 错误信息 | 原因 |
|----------|------|
| Java heap space | 堆对象太多/泄漏/堆太小 |
| GC overhead limit exceeded | 反复 GC 回收不到足够内存 |
| Metaspace | 类加载过多、CGLib 代理类、动态生成类 |
| Unable to create new native thread | 线程数爆、32 位或 ulimit |
| Direct buffer memory | DirectBuffer 未释放 |
| StackOverflowError | 无限递归（栈溢出，严格说非 OOM 但同类） |

**排查步骤：**

1. **确认现象**：容器 `OOMKilled`（dmesg / `kubectl describe pod`）还是 JVM 抛 OutOfMemoryError
2. **保留现场**：`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/`
3. **运行时**：`jstat -gcutil <pid> 1000` 看 OU、FGC 频率
4. **dump 分析**：MAT / JVisualVM 看 **Dominator Tree**，找占堆最大的对象链
5. **代码定位**：谁 hold 了引用（静态 Map、ThreadLocal 未 remove、连接未关）
6. **修复**：限大小、弱引用、分页、修复泄漏；或 **合理加堆**（泄漏未修前加堆只是延缓）

## 通俗理解：

OOM 像 **仓库爆满**：

- heap space = 货太多没出库
- Metaspace = 档案柜塞满新 SOP 手册（类太多）
- native thread = 临时工招太多（线程爆炸）

## 项目结合：

**可口述的压测故事（进销存）**：

> 并发压测 order 创建接口，容器反复重启。`docker inspect` 见 OOMKilled。  
> 初判堆设 1G 但容器 limit 也是 1G，**JVM+元空间+栈+直接内存** 超限。  
> 调整为 limit 1.5G、`-XX:MaxRAMPercentage=70` 后仍 FGC 频繁。  
> dump 发现 **InMemoryChatSessionStore** 或 **导出 List** 占堆大头 → 加会话 TTL / 改流式导出。

（若未真实发生，面试说「压测复现过 heap 压力，按上述思路排查」即可，别编造生产事故。）

## 面试官追问：

1. HeapDump 很大，生产怎么采？
2. ThreadLocal 为什么会导致泄漏？
3. `-XX:+ExitOnOutOfMemoryError` 有什么用？

## 高级回答：

- **采 dump**：低峰 **jmap -dump:live**；或 Arthas `heapdump`；live 只含存活对象，体积小些。
- **ThreadLocal 泄漏**：ThreadLocalMap 的 Entry key 是弱引用，value 强引用；**线程池线程不销毁** 时 value 一直在（如 Tomcat 场景曾经典）。
- **ExitOnOOM**：OOM 立刻退出，让 K8s/systemd **重启**，比僵死进程好；配合 dump 参数。
- 进销存 **ThreadLocal 存 LoginUser**：请求结束 `SecurityContextHolder.clearContext()`；线程池任务注意 **传递上下文后 finally 清理**。

================

---

## 题 6：JIT 编译与性能直觉

================

## 面试问题：

解释执行、JIT 编译、AOT 的关系？什么是热点代码？为什么 say「过早优化是万恶之源」但 JVM 又要 JIT？

## 考察点：

- 分层编译（C1/C2）、逃逸分析、内联
- 性能问题先度量再优化
- 和业务代码的关系（不是让背 C2 细节）

## 标准答案：

**执行路径**：

1. 字节码 **解释执行**（启动快）
2. **JIT** 统计热点方法/循环，编译为本地机器码（C1 快速编译 / C2 深度优化）
3. **AOT**（GraalVM native-image 等）：提前编译，启动快、峰值性能可能不如长期 JIT

**热点**：调用计数超过阈值 → 触发 OSR/on-stack replacement 或标准编译。

**常见 JIT 优化**：**内联**、**逃逸分析**（栈上分配/标量替换）、**锁消除**、**循环展开**。

**「过早优化」**：业务代码在 **无 profiling 证据** 前手写复杂「优化」，可读性变差；JVM JIT 是在 **运行时用数据** 做优化，层次不同。

**工程实践**：

- 先用 **Arthas trace / async-profiler / JFR** 找热点
- 算法与 IO 优化 > 纠结微观语法
- 微服务 **冷启动**：Spring Boot 启动慢主要是 **类加载+Bean 初始化**，不是 JIT  alone

## 通俗理解：

JIT 像 **老师傅看哪个工序最慢就专门练手速**：刚开始慢慢做（解释），做多了变成肌肉记忆（机器码）。

## 项目结合：

- **MyBatis + Spring** 启动 order-service 15～30s 正常，别指望 native 除非 Serverless 强需求。
- **库存扣减热点方法** 若 profiling 显示在 **JSON 序列化**，优先换更小 DTO 或 Protobuf，而不是换 GC。
- **AI 调用** 瓶颈在 **网络 RTT**，JIT 优化 Java 拼接 Prompt 几乎无感。

## 面试官追问：

1. 反编译能看到 JIT 后的代码吗？
2. `final` 对 JIT 有帮助吗？
3. 为什么循环里 `new Object()` 有时性能也不差？

## 高级回答：

- JIT 代码在 **Code Cache**；`-XX:+PrintCompilation` 可看编译事件（调试用）。
- `final` 字段有助于 **常量折叠**、减少虚方法调用，但不必滥用 final。
- 短生命周期对象在 **Eden** 分配极快（TLAB），Young GC 高效；性能问题常在 **老年代泄漏** 而非 new 本身。
- 10 年答法：**「我优化顺序：SQL/远程调用 > 算法 > 缓存 > JVM 参数；JVM 参数最后动」**。

================

---

## 题 7：强软弱虚引用与缓存设计

================

## 面试问题：

强引用、软引用、弱引用、虚引用区别？SoftReference 适合做什么？WeakReference 和 ThreadLocal 有什么关系？

## 考察点：

- ReferenceQueue、GC 交互
- 缓存场景选型（软引用 vs Caffeine/Redis）
- 不要只会背定义，要说「生产更常用什么」

## 标准答案：

| 引用 | GC 行为 | 典型用途 |
|------|---------|----------|
| 强 | 不回收 | 普通 `Object o = new` |
| 软 SoftReference | 内存紧张才回收 | **内存敏感缓存**（图片缓存，现在更常用 Caffeine） |
| 弱 WeakReference | 下次 GC 即回收 | WeakHashMap、ThreadLocal key |
| 虚 PhantomReference | 跟踪对象回收时机 | 堆外内存回收跟踪、Cleaner |

**SoftReference 缓存**：OOM 前让 GC 清缓存，但 **回收时机不可控**，高并发下仍可能 OOM；现代项目用 **Caffeine/Guava（带 size+TTL）** 或 **Redis**。

**ThreadLocal**：Entry 的 key 是 **WeakReference&lt;ThreadLocal&gt;**；key 被回收后 value 若未 remove，**线程池线程** 仍持有 value → 泄漏。

## 通俗理解：

- **强引用** = 有正式入库单，仓库不能扔
- **软引用** = 临时堆放，仓库快满时先扔这些
- **弱引用** = 标签一撕（GC）就可能清
- **虚引用** = 只关心「货什么时候真正运走」，不能拿来取货

## 项目结合：

- 进销存 **商品缓存**：用 **Redis + TTL**，不用 SoftReference Map。
- **权限菜单缓存** 在 JVM：Caffeine `maximumSize(500).expireAfterWrite(10m)` 比软引用清晰。
- **ChatSessionStore InMemory**：本质是强引用 Map，必须 **会话上限 + 过期清理**，否则等价泄漏。

## 面试官追问：

1. WeakHashMap 什么场景用？
2. finalize 为什么 deprecated？
3. Cleaner 和虚引用的关系？

## 高级回答：

- **WeakHashMap**：key 弱引用，适合做 **附加属性表**（如 ClassLoader 场景），key 无强引用时 entry 自动失效。
- **finalize** 不确定、性能差、阻碍 GC；用 **try-with-resources** 和 **Cleaner**。
- 进销存面试：**「JVM 引用我懂原理，生产缓存统一 Redis/Caffeine，可观测、可限流」** — 比背引用 API 加分。

================

---

## 题 8：容器与 JVM 参数实践

================

## 面试问题：

Docker/K8s 里跑 Spring Boot，JVM 参数怎么设？为什么容器里不能简单 `-Xmx` 等于容器 memory limit？还关注哪些指标？

## 考察点：

- 容器感知（UseContainerSupport、MaxRAMPercentage）
- OOMKilled vs heap OOM
- 可观测：heap、GC、线程、Metaspace
- 与进销存 Docker 部署结合

## 标准答案：

**容器内存组成**：

```
容器 limit
├── JVM Heap (-Xmx)
├── Metaspace
├── Thread stacks (每线程默认 ~1M × 线程数)
├── Code Cache (JIT)
├── Direct Memory
└── Native (JNI、Netty 等)
```

**因此**：`-Xmx1g` + limit 1g → **几乎必 OOMKilled**。

**推荐（JDK 11+ 容器）**：

```bash
java -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -XX:InitialRAMPercentage=75.0 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/logs/heapdump.hprof \
     -Xlog:gc*:file=/logs/gc.log:time,level,tags \
     -jar order-service.jar
```

**CPU**：`-XX:ActiveProcessorCount` 在 limit CPU 下与 cgroup 对齐；线程池大小别按物理核算满。

**关注指标**：

- `jvm_memory_used_bytes` heap / non-heap
- GC pause P99
- 线程数
- 容器 **working set** vs limit

## 通俗理解：

容器 limit 是 **整个办公室租金上限**，堆只是 **其中一个仓库** 的面积；不能把仓库划满还指望办公室能再塞员工工位和档案柜。

## 项目结合：

- **nestparts.top 宝塔 Docker order-c**：`--network host` 部署时同样要设 **memory limit** 和 JVM 百分比。
- **Gitee Go CD** 部署脚本可在 `env.sh` 加 `JAVA_OPTS`，版本化参数。
- **多微服务同机**：5 个 jar 各 MaxRAMPercentage 75% **会叠加超物理内存** — 百分比是 **单容器 limit 的 75%**，要算 **宿主机总 RAM**。
- **Prometheus + Micrometer**（若接入）：面试说「堆使用率 >85% 持续 10min 告警，FGC 次数突增告警」。

## 面试官追问：

1. JDK 8 容器有什么问题？（早期不认 cgroup limit）
2. 如何限制 Metaspace？
3. 堆外内存怎么排查？

## 高级回答：

- **JDK 8u191+** 才较好支持容器 limit；老镜像 `-Xmx512m` 手动设更稳。
- **Metaspace**：`-XX:MaxMetaspaceSize=256m`，防止类加载失控。
- **堆外**：Netty direct memory、`MaxDirectMemorySize`；NMT `-XX:NativeMemoryTracking=summary`。
- 10 年答法：结合 **「我在轻量云 Docker 跑 order，limit 1G，JAVA_OPTS 用 MaxRAMPercentage，留 25% 给非堆；上线看 GC log 和容器重启原因」** — 真实、可验证。

================

---

## 本批小结

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

下一批：**MySQL/PostgreSQL** → [面试题-04-MySQL与PostgreSQL.md](./面试题-04-MySQL与PostgreSQL.md)（待生成）· [总索引](./面试题-中高级Java后端-总索引.md)
