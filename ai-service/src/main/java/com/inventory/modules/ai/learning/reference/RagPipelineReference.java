package com.inventory.modules.ai.learning.reference;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

/**
 * 【学习用 · 非生产 Bean】LangChain「Embeddings + VectorStore + RAG」↔ Spring AI 对照。
 * <p>
 * <b>LangChain RAG 典型链（概念）：</b>
 * <pre>
 * retriever = vectorstore.as_retriever(search_kwargs={"k": 5})
 * rag_chain = (
 *   {"context": retriever, "question": RunnablePassthrough()}
 *   | prompt
 *   | llm
 * )
 * </pre>
 * </p>
 * <p>
 * <b>本项目等价（生产）：</b>
 * {@link com.inventory.modules.ai.semantic.GoodsSemanticSearchService}
 * </p>
 * <p>
 * <b>组件对照：</b>
 * <ul>
 *   <li>OpenAIEmbeddings → {@link EmbeddingModel#embed(String)}</li>
 *   <li>Chroma / FAISS → PostgreSQL 表 {@code goods_search_embedding} + pgvector</li>
 *   <li>similarity_search → SQL {@code ORDER BY embedding <=> query_vector LIMIT k}</li>
 *   <li>stuff documents into prompt → {@code GoodsSemanticRagPrompts.buildUserMessage}</li>
 *   <li>LLM generate → {@link com.inventory.modules.ai.support.AiLlmSupport#callText}</li>
 * </ul>
 * </p>
 */
public final class RagPipelineReference {

    private RagPipelineReference() {
    }

    // ======================== 阶段 1：Index（离线索引） ========================

    /**
     * LangChain: DocumentLoader → TextSplitter → Embeddings → VectorStore.add_documents
     * <p>
     * 本项目：从 {@code goods_product} 拼 chunk（名+规格+品牌+分类）→ embed → upsert。
     * 生产：{@link com.inventory.modules.ai.semantic.GoodsSemanticSearchService#reindexFromDb}
     * </p>
     */
    public static float[] indexOneChunk(EmbeddingModel embeddingModel, String chunkText) {
        // chunkText 相当于 LangChain 的 Document.page_content
        return embeddingModel.embed(chunkText);
    }

    // ======================== 阶段 2：Retrieve（在线检索） ========================

    /**
     * LangChain: retriever.get_relevant_documents(query)
     * <p>
     * 本项目：query 转向量 → pgvector TopK → List&lt;Hit&gt;
     * 生产：{@link com.inventory.modules.ai.semantic.GoodsSemanticSearchService#search}
     * </p>
     *
     * @param embeddingModel 把用户自然语言变成向量
     * @param query          用户搜索词，如「续航久的手表」
     * @param topK           取几条，LangChain 的 k=5
     */
    public static RetrieveResult retrieveTopK(
            EmbeddingModel embeddingModel,
            String query,
            int topK) {

        float[] queryVector = embeddingModel.embed(query);

        // 真实 SQL 在 GoodsSemanticSearchService；这里只描述数据结构
        // SELECT goods_id, chunk_text, 1 - (embedding <=> ?) AS score
        // FROM goods_search_embedding ORDER BY embedding <=> ? LIMIT ?

        return new RetrieveResult(queryVector, topK, List.of());
    }

    // ======================== 阶段 3+4：Augment + Generate（RAG） ========================

    /**
     * LangChain: 把 retrieved docs 塞进 prompt 的 {context}，再调 LLM。
     * <p>
     * 生产：{@link com.inventory.modules.ai.semantic.GoodsSemanticSearchService#searchWithRag}
     * </p>
     */
    public static String ragGenerate(
            ChatModel chatModel,
            String systemPrompt,
            String userQuestion,
            List<String> retrievedChunks) {

        // Augment：拼 context 块（LangChain 的 format_docs）
        StringBuilder context = new StringBuilder();
        int i = 1;
        for (String chunk : retrievedChunks) {
            context.append(i++).append(". ").append(chunk).append('\n');
        }

        // User 消息 = 问题 + 检索到的上下文（见 GoodsSemanticRagPrompts.buildUserMessage）
        String userMessage = """
                用户问题：%s

                检索到的商品片段（仅据此回答，勿编造库存数字）：
                %s
                """.formatted(userQuestion, context);

        // Generate：等价于 AiLlmSupport.callText(system, user)
        var prompt = new org.springframework.ai.chat.prompt.Prompt(List.of(
                new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                new org.springframework.ai.chat.messages.UserMessage(userMessage)
        ));
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    /** 简化 DTO，帮助理解 Retrieve 阶段输出 */
    public record RetrieveResult(float[] queryVector, int topK, List<String> chunkTexts) {
    }
}
