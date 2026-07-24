package com.inventory.modules.ai.learning.reference;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 【学习用 · 非生产 Bean】LangChain 概念 → 本项目生产类「索引表」。
 * <p>
 * 复习 AI 时打开本类，按行跳转读生产代码即可，不必背 LangChain API。
 * </p>
 */
public final class LangChainSpringAiMappingIndex {

    private LangChainSpringAiMappingIndex() {
    }

    // ========== 接口层（Spring AI 抽象 ≈ LangChain 里的 LLM/Embeddings 封装） ==========

    /** LangChain: ChatOpenAI / ChatTongyi → Spring: {@link ChatModel} */
    public static final Class<?> LLM = ChatModel.class;

    /** LangChain: OpenAIEmbeddings → Spring: {@link EmbeddingModel} */
    public static final Class<?> EMBEDDINGS = EmbeddingModel.class;

    // ========== 生产实现类（按学习顺序阅读） ==========

    /**
     * 1. 公共 LLM 工具：单次 System+User、JSON 解析。
     * LangChain 等价：LLMChain + OutputParser 的部分逻辑。
     */
    public static final String AI_LLM_SUPPORT =
            "com.inventory.modules.ai.support.AiLlmSupport";

    /**
     * 2. 多轮 Chat + Memory。
     * LangChain 等价：ConversationBufferMemory + ChatPromptTemplate。
     */
    public static final String AI_CHAT =
            "com.inventory.modules.ai.service.impl.AiChatServiceImpl";

    /**
     * 3. 结构化 JSON 输出（智搜 V1）。
     * LangChain 等价：JsonOutputParser + Pydantic。
     */
    public static final String AI_PRODUCT_PARSE =
            "com.inventory.modules.ai.service.impl.AiProductSearchServiceImpl";

    /**
     * 4. Embedding + VectorStore + RAG（智搜 V2）。
     * LangChain 等价：VectorStoreRetriever + stuff | prompt | llm。
     */
    public static final String AI_SEMANTIC_RAG =
            "com.inventory.modules.ai.semantic.GoodsSemanticSearchService";

    /**
     * 5. Tool / Agent（未落地，仅概念）。
     * LangChain 等价：@tool + AgentExecutor。
     */
    public static final String AI_TOOL_PLANNED =
            "com.inventory.modules.ai.learning.reference.ToolCallingReference";

    // ========== 文档 ==========

    /** 对照复习手册（Markdown） */
    public static final String DOC_LANGCHAIN_SPRINGAI =
            "docs/LangChain与SpringAI对照-复习手册.md";

    /** 功能总览与接口表 */
    public static final String DOC_AI_HANDBOOK =
            "docs/AI功能复习手册.md";
}
