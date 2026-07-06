# AI 智能客服 · 后端学习站点（复习用）

> 本文档把 AI 客服后端拆成 **5 个站点**，按顺序学：配置 → 入口 → 核心业务 → 会话存储 → 前后端约定。  
> 技术栈：**Spring AI 1.0.0-M6** + **通义千问 DashScope（OpenAI 兼容模式）** + **Spring Boot 3.2.5**。

---

## 一、总览：5 站地图

```
第一站  依赖 + 配置（电话线接好）
   ↓
第二站  HTTP 入口（门在哪、谁能进）
   ↓
第三站  核心业务（怎么问大模型、怎么答）
   ↓
第四站  多轮会话（怎么记住上一句）
   ↓
第五站  入参出参 + 前端联调（传什么字段）
```

| 站 | 学什么 | 核心文件 | 建议时长 | 进度 |
|----|--------|----------|----------|------|
| 第一站 | Maven 依赖、yml 配置、连通义 | `pom.xml`、`application.yml`、`AiChatProperties.java` | 30～60 min | ✅ |
| 第二站 | Controller、JWT、限流 | `AiChatController.java`、`SecurityConfig.java` | 30 min | ⬜ |
| 第三站 | Prompt 拼装、调 ChatModel | `AiChatServiceImpl.java`、`AiChatPrompts.java` | 45 min | ⬜ |
| 第四站 | sessionId、内存会话 | `ChatSessionStore.java`、`InMemoryChatSessionStore.java` | 30 min | ⬜ |
| 第五站 | DTO/VO、前端 Hook | `AiChatRequestDTO`、`AiChatResponseVO`、`useAiChat.ts` | 20 min | ⬜ |

### 一二三站速记（流程已懂版）

| 站 | 一句话 |
|----|--------|
| **第一站** | 基础配置：Spring AI + 通义，`ChatModel` + Key/降级/temperature |
| **第二站** | 谁能调：`JWT` 鉴权 + `@RateLimit` 防刷 + `@Valid` 校验参数 |
| **第三站** | 怎么调：`resolveSessionId` → 拼 **System + 历史 + User** → `callModel` → Key 空/异常 **降级** |

**第三站细项：** 首次无 sessionId 则生成并返回；不是严格「校验 session」；历史读写主要在 **第四站**。

**面试题详见：** [2.8 第一站 Q&A](#28-第一站--一问一答复习--面试) · [3.6 第二站 Q&A](#36-第二站--一问一答复习--面试) · [4.5 第三站 Q&A](#45-第三站--一问一答复习--面试)

### 代码目录（`modules/ai`）

```
modules/ai/
├── config/AiChatProperties.java      # 业务配置绑定
├── constant/AiChatPrompts.java       # System Prompt
├── controller/AiChatController.java  # HTTP 入口
├── dto/AiChatRequestDTO.java         # 请求体
├── vo/AiChatResponseVO.java          # 响应体
├── service/AiChatService.java        # 接口
├── service/impl/AiChatServiceImpl.java  # 核心实现
└── support/
    ├── ChatSessionStore.java         # 会话存储接口
    └── InMemoryChatSessionStore.java # 内存实现
```

---

## 二、第一站：依赖 + 配置

### 2.1 生活类比

| 配置 | 类比 |
|------|------|
| `pom.xml` | 给公司装电话总机（Spring AI 设备） |
| `spring.ai.openai` | 总机拨号规则：打给哪家、用什么账号 |
| `inventory.ai.chat` | 自己的规定：记几轮对话、占线说什么 |

### 2.2 Maven（`pom.xml`）

**版本号：**

```xml
<spring-ai.version>1.0.0-M6</spring-ai.version>
```

**BOM 统一版本：**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring-ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

**Starter（自动创建 `ChatModel` Bean）：**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

**Milestone 仓库（M6 不在 Maven Central 正式版）：**

```xml
<repository>
    <id>spring-milestones</id>
    <url>https://repo.spring.io/milestone</url>
</repository>
```

**为什么叫 openai 却接通义？**  
通义 DashScope 提供 **OpenAI 兼容 API**，Spring AI 的 OpenAI Starter 本质是「按 OpenAI 协议发 HTTP」，换 `base-url` 即可，不必单独写通义 SDK。

### 2.3 框架配置（`application.yml` → `spring.ai.openai`）

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY:}   # 环境变量，勿写进 Git
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-turbo
          temperature: 0.3
```

| 配置项 | 含义 |
|--------|------|
| `api-key` | 从环境变量 `DASHSCOPE_API_KEY` 读取；末尾 `:` 表示无变量时用空串 |
| `base-url` | 呼叫中心地址；Starter 会自动拼 `/v1/chat/completions` |
| `model` | 模型名；客服用 `qwen-turbo`（快、便宜） |
| `temperature` | 0～1；客服用 **0.3** 求稳定准确，少瞎编 |

**Windows 配置 Key：**

```powershell
# 临时
$env:DASHSCOPE_API_KEY = "sk-xxx"

# 永久：系统环境变量 → 新建 DASHSCOPE_API_KEY → 重启 IDEA
echo $env:DASHSCOPE_API_KEY
```

### 2.4 业务配置（`inventory.ai.chat` → `AiChatProperties`）

```yaml
inventory:
  ai:
    chat:
      max-history-messages: 20
      fallback-reply: 抱歉，智能客服暂时繁忙，请稍后再试。您也可以点击上方快捷问题，或联系管理员。
```

| 配置项 | 含义 |
|--------|------|
| `max-history-messages` | 单会话最多保留 20 条 User/Assistant 消息，超出删最早的 |
| `fallback-reply` | Key 未配 / 模型调用失败时的兜底话术 |

**绑定方式：** `@ConfigurationProperties(prefix = "inventory.ai.chat")`  
yml 用 `kebab-case`，Java 用 `camelCase`，Spring Boot 自动映射。

### 2.5 启动后发生了什么

```
1. Maven 引入 spring-ai-openai-starter
2. Spring Boot 扫描 AutoConfiguration
3. 读取 spring.ai.openai.* → 创建 ChatModel Bean
4. 扫描 @ConfigurationProperties → 创建 AiChatProperties
5. AiChatServiceImpl 构造器注入 ChatModel + AiChatProperties
6. 用户请求 POST /api/ai/chat → chatModel.call(...)
```

### 2.6 第一站验证清单

| 测试 | 做法 | 预期 |
|------|------|------|
| Key 生效 | `echo $env:DASHSCOPE_API_KEY` | 有 `sk-` 开头 |
| 只测通义 | PowerShell 调 `compatible-mode/v1/chat/completions` | 有 `choices[0].message.content` |
| 项目接口 | login → Bearer token → POST `/api/ai/chat` | `code:200`，有 `reply` |
| 前端 | F12 Network 看 `/api/ai/chat` | 200；关后端变红叉 |
| 降级 | 临时去掉 Key 重启 | 返回 `fallback-reply` 固定话术 |

**PowerShell 一条龙：**

```powershell
$login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method Post -ContentType "application/json" `
  -Body '{"userName":"你的用户名","password":"你的密码"}'
$token = $login.data.token

Invoke-RestMethod -Uri "http://localhost:8080/api/ai/chat" `
  -Method Post -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body '{"message":"如何查看库存？"}'
```

**Knife4j 注意：** 4.x 左侧「Authorize」填 token；调试时若仍 1102，在「请求头部」手动加 `Authorization: Bearer token`，或直接用 PowerShell。

### 2.7 第一站要背什么（速记卡）

**一句话方案：**

> 智能客服用 **Spring AI** 接入**通义千问（阿里云百炼 DashScope）**，走 **OpenAI 兼容端点**；Key 用 **`DASHSCOPE_API_KEY`**；Starter 自动配置 **`ChatModel`**；失败走 **`fallback-reply`** 降级。

**必背 4 配置：** `base-url` · `qwen-turbo` · `temperature:0.3` · Key 环境变量  
**必背 1 Bean：** `ChatModel` → `chatModel.call(new Prompt(...))`  
**必背 4 自测：** 为什么 openai-starter 能接通义 · Key 放哪 · 谁创建 ChatModel · 失败用户看到什么

**不必死背：** pom 仓库 XML 全文、PowerShell 脚本全文、Knife4j 操作细节  
**不属于第一站：** System Prompt（第三站）、sessionId（第四站）、JWT（第二站）

---

### 2.8 第一站 · 一问一答（复习 & 面试）

#### Q1：第一站到底要做什么？我理解「引依赖、写配置、调接口、调试」对吗？

**答：** 大方向对。更准确地说：

> **第一站 = 用 Maven 引入 Spring AI，用 yml 告诉它怎么连通义，再用业务配置定降级规则；Starter 自动注入 `ChatModel`，验证「电话线」能打通。**

补充两点：

1. **配置分两层**：`spring.ai.openai`（连模型）+ `inventory.ai.chat`（历史条数、降级话术）
2. **「调接口」**在第一站指**验证能连通义**（终端 / Network）；完整业务接口 `POST /ai/chat` 在第二～五站

---

#### Q2：介绍一下 Spring AI？

**答：** **Spring AI** 是 Spring 官方推出的 **AI 应用开发框架**，目标和 Spring Data、Spring Security 一样：在 Spring Boot 里 **用统一、工程化的方式接大模型**。

**核心能力（本项目用到的）：**

| 能力 | 说明 |
|------|------|
| **ChatModel** | 对话模型统一接口，`call(Prompt)` 拿回复 |
| **Prompt / Message** | 封装 System、User、Assistant 等消息 |
| **Starter 自动配置** | 读 yml → 创建 Bean，不用手写 HTTP |
| **OpenAI 兼容** | 可连 OpenAI，也可改 `base-url` 连通义等 |

**生活类比：** Spring Data 统一各种数据库；Spring AI 统一各种大模型——业务依赖抽象，换模型主要改配置。

**版本要求：** Spring AI 1.x 面向 **Spring Boot 3 + Java 17+**（本项目：Boot 3.2.5 + JDK 17 + M6）。

---

#### Q3：DashScope 和 ChatModel 分别是什么？什么关系？

**答：**

| | DashScope | ChatModel |
|--|-----------|-----------|
| **是什么** | **阿里云百炼**平台，通义千问的 **云端 HTTP API** | **Spring AI** 提供的 **大模型调用抽象接口** |
| **在哪** | 阿里云服务器 | 你项目 JVM 里（Bean） |
| **配置** | `base-url` + `DASHSCOPE_API_KEY` | Starter 自动注册，不用配类名 |
| **代码** | 一般不直接 import | `AiChatServiceImpl` 注入后 `call()` |
| **类比** | 外包呼叫中心（通义） | 公司统一总机接口 |

**关系：**

```
ChatModel（Spring 接口）
  → OpenAiChatModel（Starter 实现，按 OpenAI 协议发 HTTP）
  → DashScope compatible-mode
  → 通义 qwen-turbo
```

**记一句：** DashScope = 通义的电话局；ChatModel = Spring 里打电话的统一方式。

---

#### Q4：ChatModel 是不是 Spring AI 提供的「多轮对话总接口」？

**答：** **不完全是，需要修正。**

- **ChatModel** = 「**单次调用大模型**」的统一接口：你给它一个 `Prompt`（里面可以有多条消息），它返回**这一次**的回答。
- **多轮对话** = **我们项目自己做的**（第四站）：`sessionId` + `ChatSessionStore` 存历史，每次把 System + 历史 + 本轮问题拼进 `Prompt`。

**更准确的说法：**

> ChatModel 是 Spring AI 的 **对话模型调用接口**；多轮是业务层用 session + 历史消息拼 Prompt 实现的，**不是 ChatModel 内置能力**。

**易错字：** 是 **百炼**，不是「百联」。

---

#### Q5：什么是 OpenAI 兼容端点？

**答：** **OpenAI 兼容端点** = 接口的 **URL、请求 JSON、响应 JSON** 按 OpenAI Chat Completions 规范设计；客户端按 OpenAI 方式发请求，服务器可以是通义而不是 OpenAI。

**项目配置：**

```yaml
base-url: https://dashscope.aliyuncs.com/compatible-mode
# Starter 自动拼：/v1/chat/completions
```

**实际请求形态：**

```json
{
  "model": "qwen-turbo",
  "messages": [{ "role": "user", "content": "如何查看库存？" }],
  "temperature": 0.3
}
```

**为什么用兼容模式？**

| 方式 | 缺点 |
|------|------|
| 通义原生 SDK | 和 Spring AI 两套代码 |
| **OpenAI 兼容 + Spring AI** | 只改 `base-url` / `model`，业务只认 `ChatModel` |

**记一句：** OpenAI 兼容端点 = 通义用 OpenAI 的「插头标准」接 Spring AI 这根线。  
**面试：** pom 里叫 `openai-starter` 却能接通义，因为通义提供了 compatible-mode。

---

#### Q6：为什么选 Spring AI？为什么选通义千问？

**答：**

**选 Spring AI：**

| 理由 | 说明 |
|------|------|
| 技术栈统一 | Boot 3 项目，Starter + 注入，和 Security、Redis 一致 |
| 抽象清晰 | 业务依赖 `ChatModel`，不手写 HTTP |
| 换模型成本低 | 改 yml，少改代码 |
| 和 LangChain4j 分工 | 客服/SQL 用 Spring AI；智搜 RAG 用 LangChain4j |

**选通义千问（DashScope）：**

| 理由 | 说明 |
|------|------|
| 国内可用 | 百炼平台，中文文档，不用翻墙 |
| OpenAI 兼容 | 直接配 Spring AI openai-starter |
| 成本 | `qwen-turbo` 快且便宜，客服够用 |
| 已落地 | Key 已配、已测通 |

**组合一句话：**

> Spring AI 解决「Java 后端怎么工程化接 AI」；通义解决「国内用什么模型、成本低」；OpenAI 兼容端点把两者连起来。

**30 秒口述版：**

> 项目是 Spring Boot 3，用 Spring AI 的 ChatModel 抽象和 Boot 集成一致；通义百炼国内可用、有 OpenAI 兼容 API、turbo 成本低，适合客服类高频调用；智搜 RAG 计划 LangChain4j，按场景分工。

---

#### Q7：Spring Boot 2 或 JDK 7/8 能接 AI 吗？

**答：**

| 组合 | Spring AI | 接通义/大模型 |
|------|-----------|---------------|
| **Boot 3 + JDK 17**（本项目） | ✅ | ✅ |
| Boot 2 + JDK 8 | ❌ Spring AI | ✅ SDK 或 HTTP |
| JDK 7 | ❌ | ❌ 不现实 |

**要点：**

- **Spring AI 1.x 要求 Boot 3 + Java 17+**
- **Boot 3 最低 JDK 17**，不能 JDK 8
- 老项目（Boot 2 + JDK 8）可以 **DashScope SDK 或 RestTemplate/OkHttp** 调 OpenAI 兼容 API，自己管 session 和降级

**面试一句：**

> Spring AI 要 Boot 3 和 JDK 17；老系统接大模型用 SDK 或 HTTP，不用 Spring AI。

---

#### Q8：对比过 GPT-4 吗？（诚实边界 ①）

**答：**

> 项目是 **进销存智能客服**，做 **操作指引**，不是复杂推理。更看重：国内稳定访问、中文、低延迟、低成本。  
> 因此选 **qwen-turbo**，没有为本项目做 GPT-4 与通义的完整评测。  
> 若质量不够，可升 **qwen-plus/max**，或优化 Prompt / 接 RAG 灌菜单文档。

**别说：**「通义比 GPT-4 强」或「随便选的」。  
**可说：** 客服 FAQ 类任务 turbo 性价比最高；复杂场景再换更强模型。

---

#### Q9：Spring AI 还在 M6，生产敢用吗？（诚实边界 ②）

**答：**

> 用的是 **Spring AI 1.0.0-M6**（**Milestone 里程碑**，非 GA 正式版）。  
> 个人项目 / 求职 demo 为和 Boot 3.2 快速集成而选；核心逻辑依赖 `ChatModel` 接口，便于以后升 GA。  
> 公司生产若要求只用 GA，可升正式版或改用 DashScope SDK / HTTP 等价实现。  
> Maven 需配 **spring-milestones** 仓库拉 M6 依赖。

| 类型 | 含义 |
|------|------|
| M（Milestone） | 功能基本齐，API 可能微调 |
| GA | 正式版，相对稳定 |

---

#### Q10：为什么不全用 LangChain4j？（诚实边界 ③）

**答：**

> **按场景分工，不是二选一。**

| 场景 | 选型 | 原因 |
|------|------|------|
| 智能客服 | Spring AI | Prompt + 多轮，链路简单，Boot 集成好 |
| SQL 助手 | Spring AI | Text-to-SQL + 校验 |
| 商品智搜 RAG | LangChain4j | 切片、Embedding、向量检索，链式编排更顺手 |

**类比：** Spring AI = 前台直接答常见问题；LangChain4j = 先查资料库（RAG）再答。

---

#### Q11：为什么用 openai-starter 能接通义？

**答：** 通义百炼提供 **OpenAI 兼容端点**（`compatible-mode`），Starter 按 **OpenAI 协议**发 HTTP；配置 `base-url` 指向 DashScope、`api-key` 用 `DASHSCOPE_API_KEY` 即可，**无需通义专用 SDK**。

---

#### Q12：API Key 放哪？为什么不能写进 yml？

**答：** 放环境变量 **`DASHSCOPE_API_KEY`**。yml 写 `${DASHSCOPE_API_KEY:}`。  
原因：防泄露、不进 Git；改 Key 不用改代码；IDE/服务器分别配置。

---

#### Q13：启动后谁创建模型客户端？业务怎么用？

**答：** `spring-ai-openai-starter` **自动配置** `ChatModel` Bean（实现类一般为 `OpenAiChatModel`）。  
业务 `AiChatServiceImpl` **构造器注入** `ChatModel`，调用 `chatModel.call(new Prompt(messages))`，**不用自己 new 客户端**。

---

#### Q14：模型调用失败，用户看到什么？

**答：** 不是 500 白屏，返回 **`inventory.ai.chat.fallback-reply`** 固定话术（如「抱歉，智能客服暂时繁忙…」），HTTP 仍为业务成功 `code:200`（优雅降级）。  
Key 未配置时同样走降级，服务可正常启动。

---

#### Q15：第一站怎么验证通过了？

**答：**

| 验证项 | 做法 | 预期 |
|--------|------|------|
| Key | `echo $env:DASHSCOPE_API_KEY` | 有 `sk-` |
| 后端接口 | login → Bearer → POST `/api/ai/chat` | 200 + reply |
| 前端 | F12 Network `/api/ai/chat` | 200 |
| 真接口 | 关后端再发请求 | 失败（非 mock） |
| 降级 | 去掉 Key 重启 | fallback 固定话术 |

Knife4j 若 1102 且 msg 空：请求未带 `Authorization: Bearer`；用 PowerShell 或前端更可靠。

---

#### Q16：temperature 为什么用 0.3？

**答：** 范围 0～1。**客服要步骤准确、少瞎编**，用低 temperature（0.3）求稳定；创意写作才用 0.7～1.0。

---

#### Q17：第一站和第二～五站的边界？

**答：**

| 第一站 | 第二～五站 |
|--------|------------|
| 依赖 + yml + ChatModel 能连通义 | Controller、JWT、限流 |
| Key、降级、temperature | Prompt 拼装、Service |
| 终端/Network 验证 | sessionId、DTO、前端 |

---

### 2.9 第一站 · 复习自测（闭卷 4 题）

1. 为什么 openai-starter 能接通义？  
2. Key 放哪？为什么不能写 yml？  
3. ChatModel 和 DashScope 分别在什么层？  
4. 多轮对话是谁实现的？Spring AI 还是业务层？

<details>
<summary>参考答案（点击展开）</summary>

1. DashScope 提供 OpenAI 兼容端点，改 base-url 即可。  
2. 环境变量 `DASHSCOPE_API_KEY`；防泄露、不进 Git。  
3. ChatModel 在 Spring 应用内（接口/Bean）；DashScope 在云端 API。  
4. **业务层**：sessionId + ChatSessionStore 拼历史进 Prompt；ChatModel 只负责单次 call。

</details>

---

## 三、第二站：HTTP 入口

### 3.1 生活类比

Controller = 门店「咨询台窗口」：顾客（已登录用户）递纸条，窗口转给 Service，再把答复递回去。

### 3.2 接口定义

**文件：** `AiChatController.java`

| 项 | 值 |
|----|-----|
| 完整 URL | `POST http://localhost:8080/api/ai/chat` |
| 类路由 | `@RequestMapping("/ai")` |
| 方法 | `@PostMapping("/chat")` |
| context-path | `/api`（见 `application-dev.yml`） |

### 3.3 注解说明

```java
@Tag(name = "AI 智能客服")           // Knife4j 分组
@RateLimit(limit = 30, period = 60) // 每 IP 每分钟 30 次，防刷 Token
@PostMapping("/chat")
public Result<AiChatResponseVO> chat(@Valid @RequestBody AiChatRequestDTO dto) {
    return Result.success(aiChatService.chat(dto));
}
```

| 注解 | 作用 |
|------|------|
| `@Valid` | 触发 DTO 校验（message 不能为空等） |
| `@RateLimit` | Redis 计数限流（项目已有 AOP 切面） |
| `Result.success()` | 统一响应 `{ code:200, data:{...} }` |

### 3.4 JWT 鉴权

**`/ai/chat` 不在白名单**，必须登录。

**白名单（节选）：** `/auth/login`、`/auth/register`、`/doc.html` …  
**见：** `SecurityConfig.java` → `WHITE_LIST`

**请求头格式：**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**1102 错误对照：**

| 响应 | 含义 |
|------|------|
| `1102` + **msg 空** | 没带 `Authorization: Bearer ...`（Knife4j 常见） |
| `1102` + msg 有内容 | Token 过期 / 无效 / Redis 无登录态 |
| `200` | 成功 |

**鉴权链路：**

```
请求 → JwtAuthenticationFilter
     → 解析 Bearer Token
     → 校验 JWT 签名与过期
     → Redis 查登录态
     → 设置 SecurityContext
     → 进入 Controller
```

### 3.5 第二站速记

- 路由：`POST /api/ai/chat`（context-path `/api` + `@RequestMapping("/ai")` + `@PostMapping("/chat")`）
- 不在 Security 白名单 → 必须 `Authorization: Bearer {token}`
- Controller **只转发** Service，不调模型
- `@RateLimit(30, 60)`：每 IP 每分钟 30 次
- `@Valid`：`message` 不能为空（DTO 校验）

### 3.6 第二站 · 一问一答（复习 & 面试）

#### Q1：AI 客服接口完整 URL 是什么？Controller 做了什么？

**答：** `POST http://localhost:8080/api/ai/chat`。  
`AiChatController` 职责：**接 HTTP 请求** → `@Valid` 校验 DTO → `@RateLimit` 限流 → 调用 `aiChatService.chat(dto)` → `Result.success` 包装返回。**不写 Prompt、不调 ChatModel**。

---

#### Q2：为什么 `/ai/chat` 必须登录？怎么带 Token？

**答：** `SecurityConfig` 白名单只有 `/auth/login` 等，`/ai/**` 需认证。  
请求头：`Authorization: Bearer {accessToken}`（login 返回的 `data.token`）。  
防止未授权调用、刷 Token 烧模型费用。

---

#### Q3：1102 是什么？msg 为空和有内容有什么区别？

**答：** 项目约定的 **Token/鉴权失败** 码。

| 响应 | 常见原因 |
|------|----------|
| `1102` + **msg 空** | 未带 `Authorization: Bearer ...`（Knife4j 调试常见） |
| `1102` + **msg 有字** | Token 过期、无效、Redis 无登录态 |
| `200` | 鉴权通过 |

鉴权链路：`JwtAuthenticationFilter` → 解析 Bearer → 校验 JWT → Redis 查登录态 → 设置 `SecurityContext` → 进 Controller。

---

#### Q4：AI 接口为什么要 `@RateLimit`？

**答：** 调大模型 **耗 Token、要钱、有 QPS 限制**。  
本项目：`@RateLimit(limit = 30, period = 60)`，Key 为 `limit:{uri}:{ip}`，Redis INCR 计数。  
与 JWT 配合：**JWT 防未登录，限流防刷接口**。

---

#### Q5：`@Valid` 校验什么？和 Service 里降级有什么区别？

**答：** 校验 **HTTP 入参**：`message` 非空、长度 ≤2000 等（`AiChatRequestDTO`）。  
失败在 **进 Service 之前** 返回参数错误。  
Service 里降级是 **Key 空 / 模型调用失败**，仍可能返回 200 + fallback 话术——二者不是同一层。

---

#### Q6：Controller 和 Service 怎么分工？

**答：**

| 层 | 职责 |
|----|------|
| Controller（第二站） | HTTP、JWT 之后、限流、参数校验、统一 Result |
| Service（第三站） | sessionId、Prompt、ChatModel、降级、写历史 |

---

#### Q7：context-path 和前端 baseURL 怎么拼？

**答：** `application-dev.yml` 里 `context-path: /api`；Controller 类上 `/ai`、方法 `/chat`。  
前端 axios `baseURL: /api`，请求写 `/ai/chat`，**不要重复写两个 `/api`**。

---

### 3.7 第二站 · 闭卷自测（4 题）

1. `/ai/chat` 为什么在 Security 白名单外？  
2. 1102 且 msg 为空最常见原因？  
3. `@RateLimit(30, 60)` 含义？  
4. Controller 为什么不直接 `chatModel.call()`？

<details>
<summary>参考答案</summary>

1. 仅允许登录用户调用，防未授权与滥用。  
2. 请求未带 `Authorization: Bearer token`。  
3. 同一 IP 对该 URI 60 秒内最多 30 次。  
4. 分层：Controller 管 HTTP，Service 管 AI 业务。

</details>

---

## 四、第三站：核心业务

### 4.1 生活类比

| 步骤 | 类比 |
|------|------|
| 取历史 | 店员翻小本子 |
| 拼 Prompt | 岗位手册 + 聊天记录 + 本轮问题 |
| `chatModel.call()` | 外包呼叫中心回话 |
| `appendTurn` | 更新便签本 |

### 4.2 主流程（`AiChatServiceImpl.chat`）

```
1. resolveSessionId   → 没传就 new UUID
2. 检查 apiKey        → 空则 fallback-reply
3. callModel          → 拼 Prompt + 调通义
4. appendTurn         → 写入会话历史
5. 返回 sessionId + reply
```

### 4.3 Prompt 结构

```
SystemMessage（AiChatPrompts.CUSTOMER_SERVICE_SYSTEM）
    +
历史消息（ChatSessionStore.getHistory）
    +
UserMessage（本轮用户输入）
    ↓
new Prompt(messages) → chatModel.call(...)
```

**System Prompt 要点（`AiChatPrompts.java`）：**

- 角色：兔子小助手，进销存客服
- 模块：商品、库存、订单、收银、系统管理
- 要求：步骤清晰、不编造数字、不做写操作、约 200 字内

**常见问题：** 模型爱重复自我介绍 → Prompt 可加「不要重复介绍，直接给操作步骤」；可把真实菜单名（如「新建订单」）写进 Prompt。

### 4.4 第三站速记

```
resolveSessionId（无则 UUID，响应带回）
    ↓
apiKey 空？ → fallback（不调模型）
    ↓
callModel：System + History + User → chatModel.call()
    ↓
appendTurn 写历史（与第四站交界）
    ↓
异常 / 空 reply → fallback + 日志
```

### 4.5 第三站 · 一问一答（复习 & 面试）

#### Q1：`AiChatServiceImpl.chat()` 主流程是什么？

**答：**

1. `resolveSessionId`：没传 sessionId 则 `UUID` 生成，**返回给前端**  
2. 检查 `apiKey`：空则 **不调模型**，直接 `fallback-reply`  
3. `callModel`：拼 Prompt → `chatModel.call()`  
4. `appendTurn`：成功后将本轮 User/Assistant 写入 Store  
5. `catch` 异常或 reply 空 → fallback，**HTTP 仍 200**（优雅降级）

---

#### Q2：Prompt 怎么拼？三种消息顺序？

**答：** 大模型 **无状态**，每次靠应用层把上下文塞进 `messages`：

```
① SystemMessage  → AiChatPrompts（岗位手册：角色、模块、防幻觉）
② History        → chatSessionStore.getHistory(sessionId)
③ UserMessage    → 用户本轮 message
```

然后 `chatModel.call(new Prompt(messages))`，取 `response.getResult().getOutput().getText()`。

---

#### Q3：System Prompt 干什么？为什么要写进代码？

**答：** 固定 **角色**（兔子小助手）、**业务范围**（进销存模块）、**回答规范**（步骤、不编造库存数字、不做写操作）。  
减少幻觉、无关话题；与前端快捷问题一致。  
改 Prompt 后需 **重启后端** 生效。

---

#### Q4：sessionId 是「校验」还是「生成」？

**答：** 第三站是 **解析或生成**，不是像 JWT 那样校验合法性：  
- 没传 → 新建 UUID  
- 传了 → trim 后直接用  
若 Store 里没有该 id 的历史（如重启后），当 **新上下文**，不会报错。

---

#### Q5：哪些情况走降级（fallback）？用户看到什么？

**答：**

| 情况 | 行为 |
|------|------|
| `DASHSCOPE_API_KEY` 未配置 | 不调模型，返回 yml 固定话术 |
| `chatModel.call` 抛异常 | catch，ERROR 日志，fallback |
| 模型返回空文本 | fallback |
| 正常 | 通义生成的 `reply` |

对用户：**仍可能是 code 200**，`reply` 为「抱歉，智能客服暂时繁忙…」，不是 500 白屏。

---

#### Q6：为什么 `appendTurn` 放在 `callModel` 成功之后？

**答：** 只有 **模型成功回复** 才记入历史；失败时不写入，避免历史里留下「无回答的用户句」，污染下一轮 Prompt。

---

#### Q7：大模型有记忆吗？多轮谁实现？

**答：** **没有。** 每次 HTTP 独立。  
多轮 = 业务层 `sessionId` + `ChatSessionStore` 存 User/Assistant，下次 `getHistory` 拼进 Prompt。  
**ChatModel 不负责多轮**；第四站管存取与裁剪。

---

#### Q8：Controller 和 Service 在 AI 链路上的位置？

**答：** 请求过 **第二站** JWT/限流/校验 → **第三站** Service 调模型 → 响应 `{ sessionId, reply }` 再经 Controller 包 `Result`。

---

#### Q9：temperature 0.3 和 Prompt 里「200 字以内」有什么关系？

**答：** temperature 控制 **随机性**（低=稳定）；Prompt 控制 **内容与格式**。  
客服要准确步骤，两者配合：**低 temperature + 严格 System Prompt**。

---

#### Q10：模型总自我介绍、按钮名不准怎么办？

**答：** **Prompt 工程问题**，不是接口 bug。  
可在 System 加：「不要重复自我介绍；直接给步骤」；写入真实菜单名（如「新建订单」）。  
更高精度可 **RAG 灌菜单文档**（智搜模块，非本客服 MVP）。

---

### 4.6 第三站 · 闭卷自测（4 题）

1. Prompt 三种消息顺序？  
2. Key 空时会不会调用通义？  
3. 调用异常时 HTTP code 一定 500 吗？  
4. 多轮对话是 ChatModel 内置的吗？

<details>
<summary>参考答案</summary>

1. System → History → User。  
2. 不会，直接 fallback。  
3. 不一定；业务层 catch 后仍可能 200 + fallback。  
4. 不是；业务 session + Store 拼历史。

</details>

---

## 五、第四站：多轮会话

### 5.1 sessionId 是什么

| 场景 | sessionId |
|------|-----------|
| 首次对话不传 | 服务端生成 UUID，响应里返回 |
| 后续请求带上 | 同一 id → 读历史 → 多轮上下文 |
| 前端 | `useRef` 存 sessionId，自动回传 |

### 5.2 存储实现（`InMemoryChatSessionStore`）

- `ConcurrentHashMap<String, List<Message>>`
- 每会话 `synchronized` 追加 User + Assistant
- `trimHistory`：超过 `max-history-messages` 删最早消息

### 5.3 局限与升级

| 现状 | 问题 | 生产方案 |
|------|------|----------|
| JVM 内存 | 重启丢失 | Redis + TTL |
| 单机 | 多实例不共享 | Redis 集中存储 |

### 5.4 第四站知识点（面试）

- 无状态 HTTP + 有状态 sessionId
- 历史裁剪控制 Token 费用
- `ChatSessionStore` 接口可替换实现（策略模式）

### 5.5 第四站 · 一问一答（复习 & 面试）

#### Q1：第四站管什么？和第三站边界？

**答：**

| 第三站 | 第四站 |
|--------|--------|
| `resolveSessionId`、拼 Prompt、`callModel` | `getHistory` / `appendTurn` / `trimHistory` |
| 决定 **什么时候** 读写历史 | 决定历史 **存哪、怎么裁** |

---

#### Q2：`ChatSessionStore` 三个方法干什么？

**答：**

- `getHistory(sessionId)`：读该会话 User/Assistant 列表（不含 System）  
- `appendTurn(sessionId, user, assistant)`：追加一轮并裁剪  
- `clear(sessionId)`：清空（预留「新对话」）

---

#### Q3：内存实现为什么用 `ConcurrentHashMap` + `synchronized`？

**答：** Map 保证 **不同 session 并发安全**；同一 session 的 List 在 `appendTurn` 里 **synchronized**，避免同一会话并发写乱序。

---

#### Q4：单会话会 OOM 吗？真正风险是什么？

**答：** 单会话最多 **20 条**消息，体积很小。  
风险是 **sessionId 无限增多**（无 TTL、无总上限），Map 只增不减 → 会话多了可能 OOM；且 **重启丢失、多实例不共享**。

---

#### Q5：生产环境对话历史怎么存？用向量库吗？

**答：**

| 方案 | 用途 |
|------|------|
| **Redis + TTL** | 生产首选，多实例共享，自动过期 |
| **PostgreSQL/MySQL** | 审计、合规、长期查询 |
| **向量数据库** | ❌ **不存普通对话**；用于 **RAG** 商品文档检索（智搜） |

---

#### Q6：TTL 是什么？

**答：** **Time To Live（存活时间）**。Redis Key 到期 **自动删除**。  
例如 session 7 天无活动则清掉，防止垃圾会话占内存。

---

#### Q7：为什么限制 `max-history-messages: 20`？

**答：** 控制 **Prompt 长度** → 降 **Token 费用**、减 **延迟**、避免超模型上下文窗口。

---

#### Q8：如何升级到 Redis 且少改业务代码？

**答：** 新增 `RedisChatSessionStore implements ChatSessionStore`，`AiChatServiceImpl` **不用改**——接口抽象 / 策略模式。

---

### 5.6 第四站 · 闭卷自测（4 题）

1. 多轮上下文存在每条消息里还是 Store 里？  
2. 重启后端 sessionId 还在，历史还在吗？  
3. 生产为什么 Redis 不用 JVM 内存？  
4. 向量库能替代 Redis 存对话吗？

<details>
<summary>参考答案</summary>

1. Store 里；每次 call 前 getHistory 拼进 Prompt。  
2. id 可还在客户端，服务端内存已空，相当于失忆。  
3. 共享、TTL、不丢、可扩展。  
4. 不能；向量库做 RAG 语义检索，不是聊天流水账。

</details>

---

## 六、第五站：前后端约定

### 6.1 请求体（`AiChatRequestDTO`）

```json
{
  "message": "如何查看库存？",
  "sessionId": "可选，首次可不传"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `message` | 是 | 1～2000 字 |
| `sessionId` | 否 | 多轮时原样带回 |

### 6.2 响应体（`AiChatResponseVO`）

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": "d492b4b29d1146edb1844953be481ee2",
    "reply": "进入「库存管理」→「库存列表」..."
  }
}
```

### 6.3 前端（`useAiChat.ts`）

```
用户输入 → aiApi.chat({ message, sessionId })
        → 更新 sessionIdRef
        → typeReply 打字机展示（UX，非 SSE 流式）
```

**验证回答是否正确：**

1. Network 确认 `/api/ai/chat` 200
2. 按 AI 说的路径自己点进系统对照
3. 关后端应报错 → 证明非 mock

**注意：** 客服是操作指引，不查实时库存；按钮名可能与界面略有出入，属 Prompt 精度问题，可优化 System Prompt 或后续 RAG 灌菜单文档。

---

## 七、全链路时序图

```
用户 → 前端悬浮窗 → POST /api/ai/chat (JWT)
                         ↓
                  AiChatController（限流、校验）
                         ↓
                  AiChatServiceImpl
                    ├─ 读 session 历史
                    ├─ 拼 System + History + User
                    ├─ ChatModel → 通义千问
                    └─ 写回 session
                         ↓
                  { sessionId, reply } → 前端打字机
```

---

## 八、与其他 AI 模块的边界

| 模块 | 技术 | 状态 |
|------|------|------|
| 智能客服 | Spring AI + Prompt + 多轮会话 | ✅ 已接入 |
| 智搜 RAG | LangChain4j + Embedding + 向量检索 | 待做 |
| SQL 助手 | Spring AI Text-to-SQL + 表白名单 | 待做 |

---

## 九、复习计划建议（约 2～3 小时）

| 顺序 | 内容 | 时间 |
|------|------|------|
| 1 | 第一站：口述 yml → ChatModel 生效过程 | 15 min |
| 2 | 第二站：Controller + 1102 案例 | 30 min |
| 3 | 第三站：Service 断点跟 Prompt | 45 min |
| 4 | 第四站：sessionId + 内存 Map | 30 min |
| 5 | 第五站：DTO + 前端 Network 验证 | 20 min |

---

## 十、面试题速记索引

| 站 | 题量 | 文档位置 |
|----|------|----------|
| 第一站 | 17 题 + 4 题自测 | [§2.8](#28-第一站--一问一答复习--面试) |
| 第二站 | 7 题 + 4 题自测 | [§3.6](#36-第二站--一问一答复习--面试) |
| 第三站 | 10 题 + 4 题自测 | [§4.5](#45-第三站--一问一答复习--面试) |
| 第四站 | 8 题 + 4 题自测 | [§5.5](#55-第四站--一问一答复习--面试) |
| 第五站 | 2 题 | 下文 |

**第一站速览：** Spring AI · openai-starter 接通义 · ChatModel vs DashScope · 多轮在业务层  
**第二站速览：** POST /api/ai/chat · 1102 msg 空=无 Bearer · 限流 · @Valid  
**第三站速览：** System+History+User · 三种降级 · appendTurn 在成功后  
**第四站速览：** ChatSessionStore · 内存 OOM 风险 · Redis+TTL · 向量库不存对话  

**第五站**

1. sessionId 谁生成？→ 首次服务端 UUID，前端 ref 保存回传。  
2. 怎么验证是真 AI？→ Network 看接口；关后端应失败。

---

## 十一、和 20 天学习计划的关系

| 文档「站」 | 20 天计划 |
|-----------|-----------|
| 第一站 + 第五站前端 | **D1** 客服接入与测通 |
| 第二～四站精读 | **D2** 代码 + 面试题 |
| 智搜 / SQL | **D3～D9** |

---

*最后更新：2026-07-04 · 第一～四站 Q&A 已整理 · 与 `modules/ai` 代码同步*
