# LangChain 与 Spring AI 对照 · 复习手册

> 用途：刷抖音/面试听到 **LangChain、RAG、Agent、Tool** 时，能立刻对应到 **本项目 Spring AI 代码**。  
> 代码对照包：`ai-service/.../learning/reference/`（仅阅读，不参与生产 Bean）  
> 关联：[AI功能复习手册.md](./AI功能复习手册.md) · [AI应用开发-最小学习路线.md](./AI应用开发-最小学习路线.md)

---

## 1. 先记住一句话

| LangChain（Python 生态名词） | 本项目 Spring AI（Java） |
|------------------------------|---------------------------|
| 一种**编排大模型应用**的工具库 | Spring 官方 **AI 抽象层** + 你写的 Service |
| 链式组合 LCEL | **Prompt → ChatModel.call → 解析 → 降级**（手写，清晰可控） |
| 向量库 Chroma/FAISS | **PostgreSQL + pgvector** |
| ChatOpenAI | **ChatModel**（通义 DashScope OpenAI 兼容） |

**简历/面试说法：**  
> 未单独引入 LangChain Python 栈；Java 侧用 **Spring AI** 实现同等模式：Prompt 模板、多轮 Chat、Embedding、RAG、结构化 JSON 输出与规则降级。概念与 LangChain 文档互通，落地代码在 `ai-service`。

---

## 2. 概念对照总表

| LangChain 概念 | 干什么 | Spring AI / 本项目对应 | 生产代码入口 |
|----------------|--------|------------------------|--------------|
| **LLM** | 大语言模型 | `ChatModel` | `application.yml` → `ChatModel` Bean |
| **PromptTemplate** | 模板填变量 | 字符串常量 + `String.format` / 拼接 | `AiChatPrompts`、`GoodsSemanticRagPrompts` |
| **ChatPromptTemplate** | System + Human 消息 | `SystemMessage` + `UserMessage` + `Prompt` | `AiChatServiceImpl#callModel` |
| **Messages / Memory** | 多轮历史 | `ChatSessionStore` + `List<Message>` | `InMemoryChatSessionStore` |
| **LLMChain** | 模板 + 模型一步 | `AiLlmSupport#callText` | 智搜/SQL/补货等 |
| **OutputParser** | 解析模型输出 | Jackson + `extractJson` | `AiLlmSupport`、`AiProductSearchServiceImpl` |
| **Embeddings** | 文本 → 向量 | `EmbeddingModel#embed` | `GoodsSemanticSearchService` |
| **VectorStore** | 存向量、相似搜 | pgvector 表 + SQL `<=>` | `goods_search_embedding` |
| **Retriever** | 检索相关文档 | `search()` TopK | `GoodsSemanticSearchService#search` |
| **RAG Chain** | 检索 + 生成 | `searchWithRag` | `GoodsSemanticSearchService#searchWithRag` |
| **Tools / Functions** | 模型调 API | `FunctionCallback`（**未落地**，Step A） | `ToolCallingReference.java` |
| **Agent** | 多步自主调 Tool | **未落地**（Step B 口述） | 见学习路线 |
| **Callbacks** | 日志/监控 | `@Slf4j` + try/catch 降级 | 各 Service |

---

## 3. 四条主线（对照读代码）

### 3.1 客服 = Chat + Memory（无 RAG）

```text
LangChain:  ChatPromptTemplate | ChatModel | ConversationBufferMemory
本项目:     SystemMessage + history + UserMessage → chatModel.call → ChatSessionStore
```

读：`AiChatServiceImpl.java` · 参考：`learning/reference/ChatChainReference.java`

### 3.2 智搜 V1 = Structured Output（JSON Parser）

```text
LangChain:  PromptTemplate → LLM → JsonOutputParser / Pydantic
本项目:     System + User → call → extractJson → AiProductParseVO
```

读：`AiProductSearchServiceImpl.java` · 参考：`StructuredOutputReference.java`

### 3.3 智搜 V2 = Embeddings + VectorStore + RAG

```text
LangChain:  Embeddings → VectorStore → Retriever → stuff documents → LLM
本项目:     embed → pgvector TopK → 拼 context → AiLlmSupport.callText
```

读：`GoodsSemanticSearchService.java` · 参考：`RagPipelineReference.java`

### 3.4 补货/SQL/运维 = LLMChain + 业务降级

```text
LangChain:  LLMChain(prompt, llm)
本项目:     先查 PG 算数 → Prompt 润色 → 失败用规则结果
```

读：`AiInventoryServiceImpl`、`AiSqlServiceImpl` · 共用 `AiLlmSupport`

---

## 4. RAG 四步（面试 40 秒）

1. **Index**：商品 chunk → `EmbeddingModel.embed` → 写入 pgvector（`reindexFromDb`）  
2. **Retrieve**：用户 query 转向量 → SQL TopK（`search`）  
3. **Augment**：把命中 chunk 拼进 User Prompt（`GoodsSemanticRagPrompts`）  
4. **Generate**：`ChatModel` 生成 `ragSummary`（`generateRagSummary`）  

**边界：** 库存/价格仍以 **PG 列表接口** 为准，RAG 只辅助「为什么推荐」。

---

## 5. Tool / Agent（LangChain 常问 · 我们未做）

| 问题 | 回答要点 |
|------|----------|
| 什么是 Tool？ | 模型决定何时调业务函数（查库存、查订单） |
| LangChain 怎么做？ | `@tool` 装饰器 + AgentExecutor |
| Spring AI 怎么做？ | `FunctionCallback` / `@Bean` 注册函数描述 + 模型返回 tool_call |
| 我们项目？ | **未接入**；客服 MVP 不查库防幻觉；Step A 计划让助手调 1～2 个只读 API |
| 和 RAG 区别？ | RAG **检索文本**；Tool **调接口拿实时数据** |

阅读 stub：`learning/reference/ToolCallingReference.java`

---

## 6. 降级与护栏（比链更重要）

LangChain 教程常忽略；**中厂面试爱问**：

| 风险 | 本项目做法 |
|------|------------|
| Key 未配置 | 不调模型，yml 固定话术 |
| 超时/限流 | catch → fallback |
| JSON 乱格式 | `extractJson` + 规则降级 |
| 幻觉数字 | 库存/金额 **查库**，模型只润色或解析条件 |
| Prompt 注入 | System/User 分离；SQL 表白名单 |

---

## 7. 学习顺序（1～2 小时）

1. 读本文 §2 总表  
2. 打开 `learning/reference/` 四个 Reference 类（纯注释）  
3. 对照生产类各读一遍：`AiChatServiceImpl` → `AiProductSearchServiceImpl` → `GoodsSemanticSearchService` → `AiLlmSupport`  
4. 口述 §4 RAG 四步 + §5 Tool 边界  

---

## 8. 面试快问快答

**Q：你们用 LangChain 吗？**  
A：Java 栈用 **Spring AI**，模式等价：Prompt、Chat、Embedding、RAG；未引入 Python LangChain 运行时。

**Q：RAG 和微调？**  
A：业务知识走 **检索增强**，不上微调；向量存在 pgvector，和 ES 关键词互补。

**Q：为什么不用 Agent？**  
A：门店规模先用 **固定链路 + 降级** 更稳；Agent 留作 Tool 多步扩展，投递后可做 Step B。

**Q：Spring AI 和直接调 HTTP？**  
A：统一 `ChatModel`/`EmbeddingModel` 接口，换模型改配置；业务只写 Prompt 与解析。

---

## 9. 文件索引

| 类型 | 路径 |
|------|------|
| 对照文档 | 本文 |
| 学习 Reference | `ai-service/.../learning/reference/*.java` |
| 客服 | `AiChatServiceImpl` |
| 智搜 V1 | `AiProductSearchServiceImpl` |
| 智搜 V2 RAG | `GoodsSemanticSearchService` |
| 公共 LLM | `AiLlmSupport` |
