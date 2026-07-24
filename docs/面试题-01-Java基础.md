# 第 1 批：Java 基础面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 本批共 **8 题** · 下一批：JVM → [面试题-02-JVM.md](./面试题-02-JVM.md)

---

## 题 1：HashMap 底层结构与扩容

================

## 面试问题：

请说明 HashMap 的底层数据结构、put 流程，以及 JDK 8 相对 JDK 7 做了哪些优化？什么情况下链表会转红黑树？

## 考察点：

- 数组 + 链表/红黑树的基本理解
- hash 计算、碰撞处理、扩容 rehash
- 是否知道并发场景下的问题（为后续 ConcurrentHashMap 铺垫）
- 能否结合业务选 Map 实现

## 标准答案：

1. **结构**：JDK 8 起为 **数组（Node[] table）+ 链表 + 红黑树**。每个桶（bucket）存一个 Node；碰撞时挂链表；链表长度 ≥ 8 且数组长度 ≥ 64 时转红黑树；退化条件为树节点 ≤ 6 时退回链表。
2. **put 流程（简化）**：
   - 计算 `hash = (h = key.hashCode()) ^ (h >>> 16)`，定位 `(n-1) & hash`
   - 桶为空则直接放 Node
   - 否则遍历链表/树：key 相等则覆盖 value；否则尾插
   - 若链表长度达阈值则树化
   - 若 size > threshold（capacity × loadFactor，默认 0.75）则 **扩容 2 倍** 并 rehash
3. **JDK 8 优化**：头插改 **尾插**（避免并发扩容死链，但 HashMap 仍非线程安全）；引入红黑树降低极端 hash 碰撞下的查询 O(n)→O(log n)；hash 扰动简化。
4. **线程安全**：HashMap 非线程安全；多线程用 `ConcurrentHashMap` 或外部同步。

## 通俗理解：

HashMap 像一排 **带编号的小抽屉**（数组）。同一个编号里东西多了，就用 **链子串起来**；链子太长就改成 **平衡二叉树** 方便查找。抽屉满了就 **换一排更大的抽屉**，把东西重新编号搬过去。

## 项目结合：

进销存里 **商品 SKU 缓存 Map**、**权限菜单 path→id 映射** 常用 HashMap。

- 单机内存、读多写少、无并发写：**HashMap 足够**
- 若把「门店 ID → 库存快照」放本地 Map 且定时刷新，注意 **不要在迭代时修改**（`ConcurrentModificationException`）
- 多线程共享的「热点商品缓存」应换 **ConcurrentHashMap** 或 **Redis**，不要裸 HashMap

## 面试官追问：

1. HashMap 的 key 可以为 null 吗？value 呢？ConcurrentHashMap 呢？
2. 为什么负载因子是 0.75？
3. 重写 equals 为什么要重写 hashCode？
4. 你项目里有没有因为 Map 选型或并发修改出过错？

## 高级回答：

- **0.75** 是时间与空间的折中：太小频繁扩容，太大链表/树变长。
- **equals/hashCode 契约**：相等对象 hash 必须一致；不等对象 hash 可碰撞。业务实体如 `Goods` 若只重写 equals 不重写 hashCode，放入 HashSet/HashMap 会出现「逻辑相等但查不到」。
- **容量规划**：已知大概 1 万 SKU，可 `new HashMap<>(16384)` 减少扩容。
- **JDK 7 死链**（头插 + 并发扩容）是经典面试点；生产应直接禁止多线程写 HashMap。
- 进销存 **订单行 Map&lt;skuId, qty&gt;** 在内存组装时，key 用 Long 时注意 **Long 缓存 -128~127** 与 **拆箱 NPE**。

================

---

## 题 2：ArrayList 与 LinkedList 选型

================

## 面试问题：

ArrayList 和 LinkedList 底层区别是什么？什么场景用哪个？为什么大多数业务代码更常用 ArrayList？

## 考察点：

- 数组 vs 双向链表的时间复杂度
- CPU 缓存、内存局部性（中高级加分）
- 能否结合进销存列表查询场景说明

## 标准答案：

| 操作 | ArrayList | LinkedList |
|------|-----------|------------|
| 随机访问 get(i) | O(1) | O(n) |
| 尾部 add | 均摊 O(1)，扩容拷贝 | O(1) |
| 中间 insert/remove | O(n) 移动元素 | O(1) 改指针（需先定位 O(n)） |
| 内存 | 连续数组，缓存友好 | 节点分散，额外 prev/next 指针 |

**ArrayList**：动态数组，默认扩容约 1.5 倍。  
**LinkedList**：双向链表，也实现了 Deque，可作队列，但 **很少作为通用 List 首选**。

**选型原则**：

- 读多、分页、按索引访问 → **ArrayList**
- 头尾频繁插入删除且数据量不大 → 可考虑 LinkedList 或 **ArrayDeque**
- 实际业务 90%+ 用 ArrayList；LinkedList 在 JDK 源码注释里也偏「特殊结构」

## 通俗理解：

ArrayList 像 **一排连续座位**，找第 10 号座位直接走过去；中间加人要把后面的人挪一位。  
LinkedList 像 **手拉手排队**，中间插队改牵手就行，但找第 10 个人要从头数。

## 项目结合：

- **订单明细列表**、**库存流水列表**：MyBatis 查出来放 `List<OrderItem>`，分页展示 → **ArrayList**
- **操作日志导出**：顺序追加写 Excel → ArrayList
- 若做 **内存队列** 缓冲下单请求，更推荐 `ArrayBlockingQueue` / `LinkedBlockingQueue`，而不是 LinkedList 当队列裸用

## 面试官追问：

1. ArrayList 扩容机制？1.7 和 1.8 有区别吗？
2. `subList` 有什么坑？
3. 遍历 List 时 remove 元素会怎样？

## 高级回答：

- **扩容**：`grow(minCapacity)`，新容量 = `old + (old >> 1)`，即约 1.5 倍；大 List 可构造时指定 initialCapacity 避免多次拷贝。
- **subList 坑**：subList 是原 List 视图，结构性修改父 List 会导致 `ConcurrentModificationException`；且 subList 强引用父 List 可能 **内存泄漏**。
- **迭代删除**：用 `Iterator.remove()` 或 Java 8 `removeIf`，不要 for-each 里直接 `list.remove(i)`。
- 进销存 **批量导入商品**：预估 5000 行可 `new ArrayList<>(5000)`；LinkedList 在随机读为主的 CRUD 里几乎无优势。

================

---

## 题 3：==、equals、hashCode 契约

================

## 面试问题：

== 和 equals 的区别？为什么重写 equals 必须重写 hashCode？String 比较有什么特别之处？

## 考察点：

- 引用相等 vs 值相等
- Object 默认 equals 行为
- 集合键语义、Intern 池（适度）

## 标准答案：

1. **`==`**：基本类型比较值；引用类型比较 **内存地址**（是否同一对象）。
2. **`equals`**：Object 默认等同 `==`；应重写为 **业务语义相等**（如订单号相同即相等）。
3. **hashCode 契约**（《Java 规范》）：
   - 若 `a.equals(b)` 为 true，则 `a.hashCode() == b.hashCode()`
   - hash 相等 **不要求** equals 相等（碰撞允许）
   - 同一对象多次 hashCode 应一致（除非字段被改）
4. **String**：重写了 equals/hashCode；字面量进 **字符串常量池**；`new String("a")` 与 `"a"` 用 equals 相等但 `==` 可能不等。
5. **比较规范**：对象比较用 `Objects.equals(a, b)`；金额用 **BigDecimal.compareTo**，不用 equals（scale 不同可能 false）。

## 通俗理解：

`==` 问的是「是不是 **同一个人**」；equals 问的是「 **身份证号/业务键** 是不是同一个」。  
hashCode 像 **姓氏首字母**，同一个人首字母必须一样，才能被分到同一抽屉里快速找到。

## 项目结合：

- **订单实体**：若以 `orderNo` 作为业务唯一键，equals/hashCode 应基于 orderNo，不要基于自增 id（分布式下 id 可能不同步）。
- **JWT 里的 username** 与 DB 用户比较：用 equals，忽略大小写要显式 `equalsIgnoreCase`。
- **商品去重**：导入 Excel 时用 `Set<GoodsImportKey>`，key 必须正确实现 equals/hashCode（如 skuCode）。
- **金额**：订单总价 `BigDecimal`，比较是否为零用 `compareTo(BigDecimal.ZERO) == 0`

## 面试官追问：

1. 两个对象 hashCode 相同，equals 一定 true 吗？
2. HashMap 先比 hash 还是先比 equals？
3. Lombok `@Data` 自动生成 equals/hashCode 有什么风险？

## 高级回答：

- HashMap get：先比 **hash 与 equals(key)**；hash 不等直接下一个桶；hash 相等再 equals 比 key。
- **Lombok 风险**：实体关联懒加载字段、循环引用、父类字段是否参与；JPA 实体有时需要 **仅业务键** 参与 hashCode。
- **Long 比较**：`Long.valueOf(128) == Long.valueOf(128)` 为 false（超出缓存池），应用 equals 或 longValue 比较。
- 进销存 **幂等键**（如 clientRequestId）适合作为 equals/hashCode 核心字段，配合 Redis SETNX。

================

---

## 题 4：String 不可变与设计动机

================

## 面试问题：

String 为什么设计成不可变（immutable）？`String`、`StringBuilder`、`StringBuffer` 如何选型？

## 考察点：

- 安全性、常量池、hash 缓存、线程安全
- 性能意识（循环拼接）
- 日志、SQL、JWT 场景

## 标准答案：

**不可变原因**：

1. **字符串常量池**复用，节省内存
2. **安全**：网络地址、类名、权限路径等作为 key 不会被篡改
3. **hashCode 缓存**：创建时算一次，HashMap key 高效
4. **线程安全**：只读无需锁

**三兄弟**：

- `String`：不可变，拼接产生新对象
- `StringBuilder`：可变，**非线程安全**，单线程拼接首选
- `StringBuffer`：可变，方法 synchronized，多线程共享拼接（现在较少用，通常局部 StringBuilder 即可）

**性能**：循环里 `s += x` 会产生大量中间 String；应 StringBuilder 或 `String.join` / `String.format`（少量格式化）。

## 通俗理解：

String 像 **印好的铭牌**，改不了只能换一块新的；StringBuilder 像 **可擦写白板**，在同一个板上改。

## 项目结合：

- **操作日志 content**：拼接「用户 X 修改商品 Y」→ 单次拼接可用 format；循环组装导出 CSV 用 StringBuilder
- **AI Prompt 拼接**（RAG context）：StringBuilder 拼 chunk，避免多次 String 对象
- **JWT**：token 字符串只读传递，不可变符合安全模型
- **MyBatis 动态 SQL**：框架内部多用 StringBuilder，业务层不要手写大量 `+`

## 面试官追问：

1. `new String("abc")` 创建几个对象？
2. intern() 干什么？生产能用吗？
3. 日志里拼接大对象有什么更好的做法？

## 高级回答：

- `new String("abc")`：常量池已有 "abc" 则 **1 个堆对象**；没有则常量池 + 堆共 2 个（JDK 7+ 字符串池在堆）。
- **intern()**：谨慎；大量 intern 可能导致 **永久代/堆中池膨胀**；短固定串可接受。
- 日志用 **占位符** `log.info("orderId={}, sku={}", id, sku)`，避免 `+` 拼接与无谓 toString。
- 进销存 **订单号生成**（日期+序列）用 StringBuilder 或专用 IdGenerator，不要 SimpleDateFormat 静态共享（线程不安全，见并发批）。

================

---

## 题 5：异常体系与业务异常设计

================

## 面试问题：

Java 异常体系是怎样的？Checked 和 Unchecked 区别？你在项目里如何设计业务异常与全局处理？

## 考察点：

- Throwable / Exception / RuntimeException
- 何时用 checked、现代 Spring 项目倾向
- 统一响应、错误码、日志

## 标准答案：

```
Throwable
├── Error（OOM、StackOverflow，一般不捕获）
└── Exception
    ├── RuntimeException（unchecked：NPE、IllegalArgument、自定义 BizException）
    └── 其他 checked（IOException、SQLException… 必须 throws 或 try-catch）
```

**Checked**：编译期强制处理，适合 **可恢复的外部 IO**（读文件、网络）。  
**Unchecked**：编程错误/业务规则违反，不强制 throws。

**Spring Boot 实践**：

- 业务异常继承 `RuntimeException`，如 `BizException(code, message)`
- `@RestControllerAdvice` + `@ExceptionHandler` 转统一 `Result<T>`
- **不要**用异常做正常流程控制（如查不到用异常 vs 返回 Optional/空列表要约定一致）
- 记录 **error 级** 带 stack，业务可预期异常 **warn** 即可

## 通俗理解：

Checked 像 **必须签字的请假条**，编译器不让你溜；Unchecked 像 **闯红灯**，代码能编译，但运行会出事或被你统一拦下来。

## 项目结合：

进销存典型异常分层：

- **参数校验**：`@Valid` → `MethodArgumentNotValidException` → 400 + 字段错误
- **库存不足**：`InsufficientStockException` → 409 或业务码 40001，**不要** 500
- **权限不足**：Spring Security `AccessDeniedException` → 403
- **Feign 调用 inventory 超时**：包装为 `ServiceUnavailableException`，Gateway 可熔断
- **AI 模块**：LLM 超时 catch 后 **降级固定话术**，异常不应拖垮主链路

## 面试官追问：

1. try-catch-finally 中 return 顺序？
2. 为什么有些公司禁止 catch Exception？
3. Feign 异常如何传递到前端？

## 高级回答：

- **finally return** 会覆盖 try/catch 的 return；现代代码用 try-with-resources 减少 finally 关流。
- **catch Exception**：吞异常是大忌；至少 log + 包装再抛；区分 **可重试**（网络抖动）与 **不可重试**（参数非法）。
- Feign：`ErrorDecoder` 解析下游 JSON 错误体，还原为同类 BizException。
- 进销存 **下单链路**：inventory 扣减失败应 **明确错误码** 便于前端提示「库存不足」vs「系统繁忙」；与 **Seata/TCC** 未落地时更要靠异常 + 补偿脚本边界清晰。

================

---

## 题 6：泛型与类型擦除

================

## 面试问题：

什么是泛型擦除？`List<String>` 和 `List<Integer>` 运行时是什么关系？泛型在 API 设计里怎么用？

## 考察点：

- 编译期检查、运行时擦除
- 通配符 `? extends T` / `? super T`（PECS）
- 实际工程用法而非背定义

## 标准答案：

**擦除**：泛型信息在 **编译后擦除**，字节码里通常是 `List` + 强转；主要为兼容老字节码。

因此：

- 不能 `new T()`、不能 `instanceof List<String>`（可 `instanceof List<?>`）
- 运行时 `List<String>` 与 `List<Integer>` 都是 **同一 Class：List**

**PECS**：

- **Producer extends**：只读用 `? extends T`（如返回 `List<? extends GoodsVO>`）
- **Consumer super**：只写用 `? super T`（如收集 `List<? super Goods>`）

**工程用法**：

- 统一分页 `PageResult<T>`
- Feign `Result<T>`
- MyBatis 不用泛型擦除坑，但 DTO/VO 分层清晰

## 通俗理解：

泛型像 **包装外标签**「这里面是 String」，安检（编译器）会看标签；上车（运行时）标签撕了，只剩同一个箱子 List，取用时要自己记得 cast。

## 项目结合：

- `Result<Page<GoodsVO>>`、`Result<List<StockAlertVO>>` 统一 API 响应
- 商品导入：`ExcelReader<GoodsImportRow>` 类型安全
- **不要用 raw type**：`List list = new ArrayList()` 在进销存老代码重构时要清掉
- Redis 序列化 `RedisTemplate<String, Object>` 反序列化要注意 **类型信息**（Jackson `@class` 或固定 VO）

## 面试官追问：

1. 什么是桥方法（bridge method）？
2. `<? extends Number>` 能 add 吗？
3. 泛型和 Object 强转哪个更安全？

## 高级回答：

- **extends 不能 add**（除 null），因为编译器不知道具体子类型；**super 能 add T 及以下**，读出来只能是 Object。
- 桥方法：子类泛型方法签名擦除后与父类冲突，编译器自动生成 bridge。
- 进销存 **通用导出接口** `export(ExportType type, Query q)` 可返回 `Result<?>` 或分类型接口，避免一处 raw Map 到处强转。

================

---

## 题 7：Java 8 Stream 与 Optional 实战

================

## 面试问题：

Stream 常用操作有哪些？什么场景适合用 Stream，什么场景不适合？Optional 正确用法是什么？

## 考察点：

- map/filter/reduce/collect、并行流风险
- 可读性 vs 性能
- 避免 Optional 滥用

## 标准答案：

**常用**：

- `filter` 过滤低库存
- `map` DTO 转换
- `sorted` + `limit` TopN 商品
- `collect(Collectors.groupingBy)` 按分类统计
- `reduce` 汇总金额

**适合 Stream**：集合转换、链式过滤、与业务无复杂副作用的一次性计算。

**不适合**：

- 简单循环更清晰时
- **需要 break/复杂分支**
- **IO / DB 调用** 在 stream 里（难读、并行更危险）
- 数据量极大且 **并行流** 误用（默认 ForkJoinPool，与业务线程池打架）

**Optional**：

- 返回值可能为空时 **显式表达**
- **不要**作为字段、方法参数、集合元素（阿里规约也建议）
- 不要 `get()` 裸调，用 `orElse` / `orElseGet` / `ifPresent`

## 通俗理解：

Stream 像 **流水线分拣**：上传商品、过检、贴标签、装箱；Optional 像 **可能空的盒子**，打开前要先问有没有货。

## 项目结合：

```java
// 库存预警：筛出可用库存 < 安全线的 SKU
List<StockAlertVO> alerts = stocks.stream()
    .filter(s -> s.getUsableQty().compareTo(s.getSafetyStock()) < 0)
    .map(this::toAlertVO)
    .toList();

// 订单行汇总金额（BigDecimal 用 reduce）
BigDecimal total = items.stream()
    .map(OrderItem::getLineAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

- **AI 智搜** 返回 List 后 map 成前端 VO，合适
- **下单扣库存** 逐步 Feign + 事务，**不要用 parallelStream**
- `findById` 返回 `Optional<Goods>` 可以；不要把整个 Order 实体包 Optional 存 Redis

## 面试官追问：

1. parallelStream 什么时候反而更慢？
2. `Collectors.toMap` 重复 key 会怎样？
3. Optional.orElse 和 orElseGet 区别？

## 高级回答：

- **parallelStream 慢**：数据量小、任务轻、IO 密集；线程切换开销 > 收益。
- **toMap** 重复 key 抛 `IllegalStateException`；用 `(a,b)->a` 合并或 `groupingBy`。
- **orElse** 每次都会计算默认值；**orElseGet** 懒加载 Supplier，无值才算。
- 进销存 **看板聚合** 多表统计已在 SQL GROUP BY 完成，Java 层 Stream 做轻量组装即可，避免 **内存里扛百万行**。

================

---

## 题 8：接口、抽象类与 Java 8+ 默认方法

================

## 面试问题：

接口和抽象类的区别？Java 8 接口 default 方法解决什么问题？你在微服务模块划分里如何用接口？

## 考察点：

- 单继承 vs 多实现
- 设计原则（面向接口编程）
- Feign、Service 分层

## 标准答案：

| | 接口 | 抽象类 |
|---|------|--------|
| 继承 | 类可多实现接口 | 单继承 |
| 方法 | JDK8+ default/static；JDK9+ private 方法 | 可有实现方法 |
| 构造器 | 无 | 有 |
| 字段 | 常量 public static final | 可有成员变量 |
| 语义 | **能力/can-do** | **is-a 模板** |

**default 方法**：接口演进时不 **破坏** 已有实现类（如 `Collection.stream()`）。  
**冲突**：类继承抽象类 + 实现多接口同名 default → 类必须 override；接口间冲突实现类 override 指定。

**工程**：

- **Service 接口 + Impl** 便于 Mock 与 AOP
- **Feign Client** 即 HTTP 接口声明
- 领域能力：`StockService`、`OrderService` 接口；公共 CRUD 可抽 `BaseService` 抽象类（慎用过深继承）

## 通俗理解：

接口像 **驾照类型**（能开什么车）；抽象类像 **车辆底盘模板**（共性轮子引擎都装好，子类填壳）。default 方法像 **给老驾照持有人补一条新规定**，不用全员重考。

## 项目结合：

- **order-service** 调 **inventory-service**：`InventoryFeignClient` 接口 + `@FeignClient`
- **ai-service** 与业务解耦：`AiChatService` 接口，Impl 内聚 Prompt + ChatModel
- **ChatSessionStore** 接口 + `InMemory` / 未来 `Redis` 实现，符合 **开闭原则**
- 不建议所有 Entity 搞一层「AbstractBaseEntity」十层继承；进销存 **组合优于继承**

## 面试官追问：

1. 为什么 Java 单继承？
2. MyBatis Mapper 是接口，谁实现的？
3. default 方法和抽象类虚函数有什么区别？

## 高级回答：

- **单继承**：简化对象模型、避免菱形继承；多能力用 **接口组合**。
- **MyBatis Mapper**：JDK **动态代理**，SqlSession 调用 Mapper 方法时走 `MapperProxy` 执行 SQL。
- 进销存 **网关鉴权** 与 **服务内 @PreAuthorize** 双层：接口契约在 API 模块（dto + feign），实现留在各 service，面试可画 **依赖倒置** 图。

================

---

## 本批小结

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

下一批：**JVM** → 见 [总索引](./面试题-中高级Java后端-总索引.md)
