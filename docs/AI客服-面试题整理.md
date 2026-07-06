# AI 智能客服 · 面试题 Q&A（结合项目代码）

> 基于 `modules/ai` 与前端 `useAiChat` 整理，按五站 + 边界 + 自测组织。  
> 新名词在括号内简要解释；代码路径便于复习时跳转。

---

## 使用说明

| 符号 | 含义 |
|------|------|
| **必背** | 面试高频，建议能口述 30 秒版 |
| **加分** | 体现项目深度与诚实边界 |
| 代码路径 | 对应仓库内文件 |

**30 秒总述（开场白）：**

> 项目是**指引型**智能客服：Spring AI 1.0 M6 通过 OpenAI 兼容端点接通义 qwen-turbo，Controller 鉴权限流，Service 拼 System+历史+User 调 ChatModel，sessionId 多轮存 ChatSessionStore（默认内存，可选 Redis+TTL），失败走 fallback 降级；前端 sessionIdRef 回传、打字机展示。不查实时库存，RAG/Tool 是下一阶段。

---

## 一、方案概览与项目边界

### Q1：你们的 AI 客服是什么类型？接入完成了吗？【必背】

**答：** **指引型客服**，不是查库型。

| 能做 | 不能做 |
|------|--------|
| 回答如何操作进销存系统（菜单路径、步骤） | 查实时库存、订单号、价格 |
| 多轮对话记住上下文 | 代替用户下单、改库存 |

**接入状态：** MVP 闭环已完成——配置、接口、Prompt、会话、前端联调均可演示。

**代码依据：** `AiChatPrompts.java` 明确要求「不要编造库存数量、订单号、价格」「只提供操作指引」。

---

### Q2：整体技术栈是什么？

**答：**

| 层次 | 选型 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.2.5 + Java 17 | Spring AI 1.x 要求 Boot 3 + JDK 17+ |
| AI 框架 | Spring AI 1.0.0-M6 | Milestone 里程碑版，非 GA（General Availability，正式发布版） |
| 大模型 | 通义千问 qwen-turbo | 经阿里云百炼 DashScope 调用 |
| 协议 | OpenAI 兼容端点（compatible-mode） | 请求/响应格式与 OpenAI Chat Completions 一致 |
| 会话 | 内存（默认）或 Redis + TTL | TTL = Time To Live，Key 存活时间，到期自动删除 |
| 前端 | React + `useAiChat` Hook | 打字机为 UX，非 SSE 流式 |

---

### Q3：还没做的能力属于哪一块？和当前客服什么关系？【加分】

**答：** 分三类，**不是 MVP 漏做**：

| 未做项 | 归属 | 与客服关系 |
|--------|------|------------|
| RAG 灌菜单文档 | 智搜模块（LangChain4j + Embedding 向量化 + 向量库） | 可选增强指引准确度 |
| SQL 助手 | 独立 AI 模块 | 并列能力，非客服子功能 |
| Tool / Function Calling 查实时数据 | 客服 2.0 / 业务 AI 层 | 从指引型升级为查库型 |
| SSE 流式输出 | 第三站+第五站体验增强 | 改返回方式与前端消费 |
| 前端「新对话」 | 第四站+第五站 | 后端已有 `clear()`，缺 API 暴露 |

---

## 二、第一站：依赖 + 配置

### Q4：为什么 pom 里是 `openai-starter` 却能接通义？【必背】

**答：** 通义百炼提供 **OpenAI 兼容端点**——URL、请求 JSON、响应 JSON 按 OpenAI Chat Completions 规范设计。Spring AI 的 openai-starter 本质是「按 OpenAI 协议发 HTTP」，只需改 `base-url` 和 `api-key`，无需通义专用 SDK。

**配置（`application.yml`）：**

```yaml
spring.ai.openai:
  api-key: ${DASHSCOPE_API_KEY:}
  base-url: https://dashscope.aliyuncs.com/compatible-mode
  chat.options:
    model: qwen-turbo
    temperature: 0.3
```

Starter 会自动拼路径 `/v1/chat/completions`。

---

### Q5：Spring AI 是什么？ChatModel 和 DashScope 什么关系？【必背】

**答：**

- **Spring AI**：Spring 官方 AI 应用框架，类似 Spring Data，用统一方式接各种大模型。
- **ChatModel**：Spring AI 提供的**对话模型调用接口**，业务里 `chatModel.call(new Prompt(messages))`。
- **DashScope（百炼）**：阿里云上通义千问的**云端 HTTP API**，在服务器端，不在 JVM 里。

**关系链：**

```
ChatModel（Spring 接口）
  → OpenAiChatModel（Starter 实现）
  → HTTP → DashScope compatible-mode
  → 通义 qwen-turbo
```

**记一句：** DashScope = 通义电话局；ChatModel = 应用里打电话的统一方式。

---

### Q6：ChatModel 自带多轮对话吗？【必背】

**答：** **不带。** ChatModel 只负责**单次** `call(Prompt)`——你给它一批 messages，它返回这一次的回答。

**多轮是业务层实现的（第四站）：**

1. `sessionId` 标识会话  
2. `ChatSessionStore` 存 User/Assistant 历史  
3. 每次 `callModel` 前 `getHistory`，拼进 Prompt  

大模型 HTTP 请求**无状态**（stateless，每次独立），「记忆」靠应用层塞历史。

---

### Q7：API Key 为什么用环境变量？配置分几层？

**答：**

| 配置前缀 | 作用 | 典型项 |
|----------|------|--------|
| `spring.ai.openai.*` | 连模型 | api-key、base-url、model、temperature |
| `inventory.ai.chat.*` | 业务规则 | max-history-messages、fallback-reply、session-store、session-ttl-seconds |

Key 放 **`DASHSCOPE_API_KEY` 环境变量**，yml 写 `${DASHSCOPE_API_KEY:}`，原因：防泄露、不进 Git、改 Key 不改代码。

**业务配置绑定类：** `AiChatProperties.java`（`@ConfigurationProperties`）。

---

### Q8：temperature 为什么用 0.3？

**答：** temperature（温度参数）范围 0～1，控制输出随机性。客服要**步骤准确、少瞎编**，用 **0.3** 偏低求稳定；创意写作才用 0.7～1.0。与 Prompt 里「200 字以内」配合：一个控随机性，一个控内容与格式。

---

### Q9：Spring AI M6 生产敢用吗？【加分 · 诚实边界】

**答：** M6 是 **Milestone 里程碑版**，API 可能微调。个人项目/求职 demo 为与 Boot 3 快速集成而选；核心依赖 `ChatModel` 接口，便于升 GA。公司生产若要求正式版，可升 Spring AI GA 或改用 DashScope SDK / 原生 HTTP 等价实现。Maven 需配 `spring-milestones` 仓库。

---

### Q10：为什么选 Spring AI + 通义，不全用 LangChain4j？【加分】

**答：** **按场景分工**，非二选一。

| 场景 | 选型 | 原因 |
|------|------|------|
| 智能客服 | Spring AI | Prompt + 多轮，链路简单，Boot 集成好 |
| SQL 助手 | Spring AI | Text-to-SQL + 校验 |
| 商品智搜 RAG | LangChain4j | 文档切片、Embedding、向量检索链式编排更顺手 |

---

## 三、第二站：HTTP 入口

### Q11：AI 客服接口 URL 和 Controller 职责？【必背】

**答：**

- **URL：** `POST http://localhost:8080/api/ai/chat`  
  - context-path：`/api`  
  - 类：`@RequestMapping("/ai")`  
  - 方法：`@PostMapping("/chat")`

- **Controller 只做：** 接 HTTP → `@Valid` 校验 DTO → `@RateLimit` 限流 → 调 `aiChatService.chat(dto)` → `Result.success` 包装。**不写 Prompt、不调 ChatModel。**

**代码：** `AiChatController.java`

---

### Q12：为什么必须登录？怎么带 Token？【必背】

**答：** `/ai/chat` **不在** `SecurityConfig.WHITE_LIST` 白名单（白名单仅有 `/auth/login`、`/doc.html` 等），必须 JWT 鉴权。

**请求头：**

```
Authorization: Bearer {accessToken}
```

防止未授权调用、刷接口烧 Token 费用。

---

### Q13：1102 错误怎么排查？【必背】

**答：** 项目约定的 **Token/鉴权失败** 业务码。

| 现象 | 常见原因 |
|------|----------|
| `1102` + **msg 为空** | 未带 `Authorization: Bearer ...`（Knife4j 调试常见） |
| `1102` + **msg 有内容** | Token 过期、无效、Redis 无登录态 |
| `200` | 鉴权通过 |

**链路：** `JwtAuthenticationFilter` → 解析 Bearer → 校验 JWT → Redis 查登录态 → 设置 SecurityContext → 进 Controller。

---

### Q14：为什么 AI 接口要 `@RateLimit`？

**答：** 调大模型耗 Token、有成本、有 QPS 限制。本项目：

```java
@RateLimit(limit = 30, period = 60, msg = "AI 客服请求过于频繁，请稍后再试")
```

即同一 IP 对该 URI **60 秒内最多 30 次**（Redis INCR 计数，AOP 切面实现）。与 JWT 配合：**JWT 防未登录，限流防刷接口**。

---

### Q15：`@Valid` 校验什么？和 Service 降级有什么区别？

**答：**

| | `@Valid`（Controller） | Service 降级 |
|--|------------------------|--------------|
| 校验对象 | HTTP 入参 DTO | 模型调用结果 |
| 典型失败 | `message` 为空、超长 | Key 空、通义超时、返回空 |
| 时机 | **进 Service 之前** | Service 内部 |
| 用户感知 | 参数错误响应 | 仍可能 HTTP 200 + fallback 话术 |

**DTO 规则（`AiChatRequestDTO`）：** `message` 必填、≤2000 字；`sessionId` 可选、≤64 字。

---

## 四、第三站：核心业务

### Q16：`AiChatServiceImpl.chat()` 主流程？【必背】

**答：**

```
1. resolveSessionId   → 没传则 UUID（去横线），响应带回
2. 检查 apiKey        → 空则 fallback，不调模型
3. callModel          → System + History + User → chatModel.call()
4. appendTurn         → 成功后才写历史
5. catch 异常         → fallback + ERROR 日志，HTTP 仍可能 200
```

**代码：** `AiChatServiceImpl.java`

---

### Q17：Prompt 怎么拼？三种消息顺序？【必背】

**答：**

```
① SystemMessage  → AiChatPrompts.CUSTOMER_SERVICE_SYSTEM（岗位手册）
② History        → chatSessionStore.getHistory(sessionId)
③ UserMessage    → 用户本轮 message
```

然后 `chatModel.call(new Prompt(messages))`，取 `response.getResult().getOutput().getText()`。

**System 不存入 Store**，每次请求重新 new，保证角色规范始终最新。

---

### Q18：System Prompt 干什么？为什么要写进代码？

**答：** 固定**角色**（兔子小助手）、**业务范围**（五大模块）、**回答规范**（步骤清晰、不编造数字、不做写操作、约 200 字）。

**代码：** `AiChatPrompts.java` 常量 `CUSTOMER_SERVICE_SYSTEM`。

改 Prompt 后需**重启后端**生效（当前未做热更新）。

---

### Q19：哪些情况走 fallback 降级？【必背】

**答：**

| 情况 | 行为 |
|------|------|
| `DASHSCOPE_API_KEY` 未配置 | 不调模型，返回 yml 固定话术 |
| `chatModel.call` 抛异常 | catch，日志，fallback |
| 模型返回空文本 | fallback |

**配置：** `inventory.ai.chat.fallback-reply`

对用户：通常仍是 **code 200**，`reply` 为「抱歉，智能客服暂时繁忙…」，不是 500 白屏——这叫**优雅降级**。

---

### Q20：为什么 `appendTurn` 放在 `callModel` 成功之后？

**答：** 只有模型**成功回复**才记入历史。失败时不写入，避免历史里留下「有问无答的用户句」，污染下一轮 Prompt。

**代码顺序（`AiChatServiceImpl.chat`）：**

```java
String reply = callModel(sessionId, userMessage);
chatSessionStore.appendTurn(sessionId, userMessage, reply);
```

---

### Q21：sessionId 是校验还是生成？

**答：** **解析或生成**，不是 JWT 式合法性校验。

- 没传 → `UUID.randomUUID()` 去横线  
- 传了 → trim 后直接用  
- Store 里没有该 id 的历史（如重启后内存清空）→ 当新上下文，**不报错**

---

### Q22：模型总自我介绍、按钮名不准怎么办？【加分】

**答：** **Prompt 工程问题**，不是接口 bug。可在 System 加「不要重复自我介绍，直接给步骤」；写入真实菜单名。更高精度需 **RAG**（Retrieval-Augmented Generation，检索增强生成：先查文档再答）灌菜单，属智搜/客服增强阶段。

---

## 五、第四站：多轮会话

### Q23：第四站和第三站边界？

**答：**

| 第三站 | 第四站 |
|--------|--------|
| 何时读写历史（callModel 前后） | 历史存哪、怎么裁、怎么过期 |
| `resolveSessionId`、拼 Prompt | `getHistory` / `appendTurn` / `trimHistory` |

**接口：** `ChatSessionStore.java`  
**默认实现：** `InMemoryChatSessionStore.java`  
**可选实现：** `RedisChatSessionStore.java`

Service 只依赖接口，换存储不改业务——**策略模式**。

---

### Q24：`ChatSessionStore` 三个方法？

**答：**

| 方法 | 作用 |
|------|------|
| `getHistory(sessionId)` | 读 User/Assistant 列表（不含 System） |
| `appendTurn(sessionId, user, assistant)` | 追加一轮（2 条消息）并裁剪 |
| `clear(sessionId)` | 清空会话（预留「新对话」） |

---

### Q25：内存版为什么 ConcurrentHashMap + synchronized？

**答：**

- `ConcurrentHashMap`：不同 sessionId 并发读写安全  
- 同一 session 的 `List` 在 `appendTurn` 里 **`synchronized(history)`**：避免同一会话并发写乱序  

**代码：** `InMemoryChatSessionStore.java`

---

### Q26：单会话会 OOM 吗？真正风险是什么？【必背】

**答：** 单会话最多 **20 条**消息（`max-history-messages`），体积很小。

**真正风险：**

1. **sessionId 无限增多**——内存 Map 无 TTL、无总上限，只增不减  
2. **JVM 重启丢失**  
3. **多实例不共享**  

生产用 **Redis + TTL** 解决。

---

### Q27：生产对话历史怎么存？向量库能存对话吗？【必背】

**答：**

| 方案 | 用途 |
|------|------|
| **Redis + TTL** | 生产首选：多实例共享、自动过期 |
| **MySQL/PostgreSQL** | 审计、合规、长期查询 |
| **向量数据库** | ❌ **不存普通聊天流水**；用于 RAG 商品/文档语义检索 |

**记一句：** Redis 存「聊天记录」；向量库存「可检索的知识片段」。

---

### Q28：TTL 是什么？Redis 版怎么刷新？

**答：** **TTL（Time To Live）** = Key 存活时间。到期 Redis **自动 DELETE** 整个 Key。

**配置：** `session-ttl-seconds: 604800`（7 天）

**刷新时机：** 每次 `appendTurn` 写回时 `SET key value EX ttl`，**重置倒计时**——类似「每次聊天给会话续期 7 天」。

**启用 Redis 存储：**

```yaml
inventory.ai.chat.session-store: redis
```

`@ConditionalOnProperty` 保证与内存版**互斥**，同一时刻只有一个 Bean。

---

### Q29：为什么限制 max-history-messages: 20？

**答：** 控制 **Prompt 长度** → 降 **Token 费用**、减延迟、避免超模型**上下文窗口**（context window，单次请求可接受的最大 token 数）。

计数单位是**消息条数**（User、Assistant 各 1 条），不是「轮数」；20 条 ≈ 10 轮。超出从**头部**删最早消息。

---

### Q30：Redis 版为什么用 ChatHistoryEntry，不直接存 Message？

**答：** Spring AI 的 `Message` 是接口/多实现类，JSON 序列化（serialization，对象转可存储格式）不稳定。用 `role + content` 的 POJO（Plain Old Java Object，简单 Java 对象）更清晰，读写时再转 `UserMessage` / `AssistantMessage`。

**反序列化兼容：** Jackson 读回时元素有时是 `ChatHistoryEntry`，有时是 `LinkedHashMap`（Map），`readEntries` 做 instanceof 分支兜底。

---

### Q31：Redis 版 appendTurn 为什么用读-改-写？并发有什么问题？【加分】

**答：** 实现简单、便于学习：`GET → 追加 → trim → SET + TTL`。

**并发风险：** 同一 session 并发 append 可能后写覆盖先写。客服场景可接受；生产高并发可改 Redis List + Lua 脚本或分布式锁。

内存版用 `synchronized` 保护同一 session；Redis 版未加锁，面试可主动说升级路径。

---

## 六、第五站：前后端约定

### Q32：请求体和响应体字段？【必背】

**请求（`AiChatRequestDTO`）：**

```json
{ "message": "如何查看库存？", "sessionId": "可选" }
```

**响应（`Result<AiChatResponseVO>`）：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": "d492b4b29d1146edb1844953be481ee2",
    "reply": "进入「库存管理」→..."
  }
}
```

| 概念 | 说明 |
|------|------|
| DTO（Data Transfer Object） | 入参，前端 → 后端 |
| VO（View Object） | 出参 data 部分，后端 → 前端 |

---

### Q33：sessionId 谁生成？前端怎么维护？【必背】

**答：**

1. 首次：不传 sessionId → 服务端 `resolveSessionId` 生成 UUID  
2. 响应：`data.sessionId` 返回前端  
3. 前端：`useAiChat.ts` 里 **`sessionIdRef`（useRef）** 保存，下次 `aiApi.chat` 带上  
4. 用 ref 而非 useState：sessionId 不需展示，避免无意义重渲染  

**API 层：** `aiApi.chat` → `POST /ai/chat`（axios baseURL 已含 `/api`，勿重复写）

---

### Q34：打字机效果是流式 AI 吗？

**答：** **不是。** 后端 `chatModel.call` **一次性**返回完整 `reply`；前端 `typeReply` 用 `setTimeout` 逐字展示，纯 **UX（用户体验）**。

**SSE（Server-Sent Events，服务端推送）** / WebSocket 流式是下一阶段，需改后端返回方式与前端消费逻辑。

---

### Q35：怎么验证是真 AI 而不是 mock？【必背】

**答：**

1. F12 Network 看真实 `POST /api/ai/chat`，带 JWT，有 JSON 响应  
2. **关掉后端**再发消息 → 应失败（mock 不依赖后端）  
3. 问不同问题，回复内容应变化  
4. 去掉 `DASHSCOPE_API_KEY` 重启 → reply 变为 fallback 固定话术  

---

### Q36：降级时前端会进 catch 吗？

**答：** **通常不会。** 降级仍返回 HTTP 200 + `code: 200` + fallback 的 `reply`，前端正常打字机展示。只有网络失败、1102 鉴权失败等才进 `catch` 显示错误文案。

---

## 七、综合与架构

### Q37：全链路时序（口述版）【必背】

**答：**

```
用户输入
  → 前端 useAiChat（sessionIdRef + message）
  → POST /api/ai/chat（JWT + RateLimit + @Valid）
  → AiChatController
  → AiChatServiceImpl
       ├─ getHistory(sessionId)
       ├─ System + History + User → chatModel.call → DashScope
       └─ appendTurn（成功时）
  → Result { sessionId, reply }
  → 前端更新 sessionIdRef + 打字机展示
```

---

### Q38：Controller 和 Service 怎么分工？

**答：**

| 层 | 职责 |
|----|------|
| Controller | HTTP、鉴权之后、限流、参数校验、Result 包装 |
| Service | sessionId、Prompt、ChatModel、降级、写历史 |
| ChatSessionStore | 历史存取与裁剪（策略可换） |

---

### Q39：和 GPT-4 对比过吗？【加分 · 诚实边界】

**答：** 项目是进销存**操作指引**，非复杂推理。更看重国内稳定、中文、低延迟、低成本，故选 qwen-turbo，未做 GPT-4 完整评测。若质量不够可升 qwen-plus/max，或 RAG + Prompt 优化。不说「通义比 GPT-4 强」。

---

### Q40：老项目 Boot 2 + JDK 8 能接 AI 吗？

**答：** **不能用 Spring AI 1.x**（要求 Boot 3 + JDK 17+）。老系统可用 DashScope SDK 或 RestTemplate/OkHttp 调 OpenAI 兼容 API，自管 session 和降级。

---

## 八、名词速查表

| 名词 | 解释 |
|------|------|
| Spring AI | Spring 官方 AI 框架，统一接大模型 |
| ChatModel | Spring AI 单次对话调用接口 |
| DashScope / 百炼 | 阿里云通义 API 平台 |
| OpenAI 兼容端点 | 请求响应格式与 OpenAI 一致，可换 base-url 接通义 |
| Prompt | 发给模型的一包 messages |
| System Prompt | 系统提示词，定角色与规则 |
| Token | 模型计费与长度单位，约 1～2 字符/Token（中文） |
| temperature | 输出随机性，0～1 |
| 无状态 HTTP | 每次请求独立，模型不记上次 |
| sessionId | 业务层会话标识，关联多轮历史 |
| fallback / 降级 | 失败时返回固定友好话术 |
| TTL | Redis Key 存活时间，到期自动删 |
| RAG | 检索增强生成，先查文档再答 |
| SSE | 服务端推送，用于流式输出 |
| DTO / VO | 入参对象 / 出参视图对象 |
| 策略模式 | 接口 + 多实现可替换（ChatSessionStore） |
| 泛型擦除 | 运行时 List 不知道元素具体类型，影响 JSON 反序列化 |
| JWT | JSON Web Token，登录鉴权 |
| 向量库 | 存 Embedding 向量，做语义检索，不存聊天流水 |

---

## 九、闭卷自测（20 题精选）

<details>
<summary>1. 为什么 openai-starter 能接通义？</summary>

DashScope 提供 OpenAI 兼容端点，改 base-url 即可。

</details>

<details>
<summary>2. 多轮对话是 ChatModel 内置的吗？</summary>

不是；业务 sessionId + ChatSessionStore 拼历史进 Prompt。

</details>

<details>
<summary>3. Prompt 三种消息顺序？</summary>

System → History → User。

</details>

<details>
<summary>4. Key 空时会调通义吗？</summary>

不会，直接 fallback。

</details>

<details>
<summary>5. 1102 且 msg 为空最常见原因？</summary>

未带 Authorization: Bearer token。

</details>

<details>
<summary>6. appendTurn 为什么在 callModel 成功之后？</summary>

避免失败时历史留下有问无答。

</details>

<details>
<summary>7. 重启后端，客户端 sessionId 还在，历史还在吗？（内存版）</summary>

id 可能在客户端，服务端内存已空，相当于失忆。

</details>

<details>
<summary>8. 向量库能替代 Redis 存对话吗？</summary>

不能；向量库做 RAG 语义检索，不是聊天流水账。

</details>

<details>
<summary>9. 当前客服能查实时库存吗？</summary>

不能，是指引型；查库需 Tool 或 RAG+业务接口。

</details>

<details>
<summary>10. 打字机说明后端流式吗？</summary>

不是；一次返回完整 reply，前端动画。

</details>

<details>
<summary>11. sessionId 存在前端哪里？</summary>

useAiChat 的 sessionIdRef。

</details>

<details>
<summary>12. @Valid 和 fallback 区别？</summary>

前者校验入参在进 Service 前；后者是模型/Key 失败的业务降级。

</details>

<details>
<summary>13. System Message 存 Redis 吗？</summary>

不存；每次 callModel 重新 new。

</details>

<details>
<summary>14. max-history-messages 单位是轮还是条？</summary>

条（User、Assistant 各算 1 条）。

</details>

<details>
<summary>15. 生产为什么 Redis 不用 JVM 内存？</summary>

共享、TTL、重启不丢、可扩展。

</details>

<details>
<summary>16. 怎么验真 AI？</summary>

Network 看接口；关后端应失败。

</details>

<details>
<summary>17. temperature 0.3 的目的？</summary>

客服要稳定准确，低随机性。

</details>

<details>
<summary>18. ChatSessionStore 换 Redis 要改 Service 吗？</summary>

不用，只改 yml session-store=redis。

</details>

<details>
<summary>19. 为什么要序列化存 Redis？</summary>

Redis 存字符串/字节，Java 对象需转 JSON 等格式。

</details>

<details>
<summary>20. MVP 已完成，RAG 属于什么？</summary>

智搜模块或客服增强，非 MVP 缺项。

</details>

---

## 十、代码索引

| 主题 | 文件 |
|------|------|
| 配置 | `application.yml`、`AiChatProperties.java` |
| HTTP 入口 | `AiChatController.java`、`SecurityConfig.java` |
| 核心业务 | `AiChatServiceImpl.java`、`AiChatPrompts.java` |
| 会话存储 | `ChatSessionStore.java`、`InMemoryChatSessionStore.java`、`RedisChatSessionStore.java`、`ChatHistoryEntry.java` |
| 入参出参 | `AiChatRequestDTO.java`、`AiChatResponseVO.java` |
| Redis 序列化 | `RedisConfig.java` |
| 前端 | `useAiChat.ts`、`api/modules/ai.ts`、`api/types/ai.ts` |

---

*最后更新：2026-07-04 · 与 `modules/ai` 及前端联调代码同步*
