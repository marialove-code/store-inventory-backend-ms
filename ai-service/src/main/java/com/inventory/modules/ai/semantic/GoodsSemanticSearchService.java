package com.inventory.modules.ai.semantic;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.ResultCode;
import com.inventory.modules.ai.support.AiLlmSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 智搜 V2：Embedding + pgvector TopK，可选 RAG（Chat 根据命中原文生成推荐说明）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inventory.ai.semantic.enabled", havingValue = "true", matchIfMissing = true)
public class GoodsSemanticSearchService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final AiLlmSupport aiLlmSupport;

    @Value("${inventory.ai.semantic.reindex-limit:200}")
    private int reindexLimit;

    @Value("${inventory.ai.semantic.top-k:5}")
    private int defaultTopK;

    @Value("${inventory.ai.semantic.embedding-dimensions:1024}")
    private int embeddingDimensions;

    @Value("${inventory.ai.semantic.rag-enabled:true}")
    private boolean ragEnabled;

    /**
     * 从 goods_product 拉商品 → 拼 chunk → Embedding → 写入 goods_search_embedding。
     *
     * @return 成功写入条数
     */
    public int reindexFromDb() {
        String sql = """
                SELECT id, product_name, spec_model, brand_name, category_name
                FROM goods_product
                WHERE COALESCE(is_deleted, 0) = 0
                ORDER BY id
                LIMIT ?
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, Math.max(reindexLimit, 1));
        if (rows.isEmpty()) {
            return 0;
        }

        int ok = 0;
        for (Map<String, Object> row : rows) {
            Long goodsId = ((Number) row.get("id")).longValue();
            String chunk = buildChunkText(row);
            if (!StringUtils.hasText(chunk)) {
                continue;
            }
            float[] vector = embeddingModel.embed(chunk.trim());
            assertDimension(vector);
            upsert(goodsId, chunk.trim(), vector);
            ok++;
        }
        log.info("智搜V2 reindex 完成 count={}", ok);
        return ok;
    }

    /**
     * 语义检索 TopK（仅向量，不含 Chat）。
     */
    public List<GoodsSemanticHitVO> search(String query, Integer topK) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "搜索词 q 不能为空");
        }
        int k = topK == null || topK < 1 ? defaultTopK : Math.min(topK, 20);

        float[] qVec = embeddingModel.embed(query.trim());
        assertDimension(qVec);
        String vectorLiteral = toVectorLiteral(qVec);

        // <=> 为余弦距离（越小越像）；score = 1 - distance 便于展示
        String searchSql = """
                SELECT goods_id, chunk_text,
                       (1 - (embedding <=> ?::vector)) AS score
                FROM goods_search_embedding
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        List<Map<String, Object>> hits = jdbcTemplate.queryForList(searchSql, vectorLiteral, vectorLiteral, k);
        List<GoodsSemanticHitVO> result = new ArrayList<>(hits.size());
        for (Map<String, Object> hit : hits) {
            Number gid = (Number) hit.get("goods_id");
            Number score = (Number) hit.get("score");
            result.add(GoodsSemanticHitVO.builder()
                    .goodsId(gid == null ? null : gid.longValue())
                    .chunkText((String) hit.get("chunk_text"))
                    .score(score == null ? null : score.doubleValue())
                    .build());
        }
        return result;
    }

    /**
     * 完整 RAG：TopK 检索 + 用命中原文生成推荐说明。
     */
    public GoodsSemanticSearchResultVO searchWithRag(String query, Integer topK) {
        String q = query == null ? "" : query.trim();
        List<GoodsSemanticHitVO> hits = search(q, topK);
        String summary;
        if (hits.isEmpty()) {
            summary = "未找到与「" + q + "」语义相近的商品，可换个说法或先重建向量索引。";
        } else if (!ragEnabled) {
            summary = GoodsSemanticRagPrompts.fallbackSummary(q, hits.size());
        } else {
            summary = generateRagSummary(q, hits);
            if (!StringUtils.hasText(summary)) {
                summary = GoodsSemanticRagPrompts.fallbackSummary(q, hits.size());
            }
        }
        return GoodsSemanticSearchResultVO.builder()
                .hits(hits)
                .ragSummary(summary)
                .build();
    }

    private String generateRagSummary(String query, List<GoodsSemanticHitVO> hits) {
        StringBuilder block = new StringBuilder();
        int i = 1;
        for (GoodsSemanticHitVO hit : hits) {
            String score = hit.getScore() == null ? "-" : String.format(Locale.US, "%.3f", hit.getScore());
            block.append(i++)
                    .append(". ")
                    .append(hit.getGoodsId())
                    .append(" | ")
                    .append(hit.getChunkText() == null ? "" : hit.getChunkText())
                    .append(" | ")
                    .append(score)
                    .append('\n');
        }
        String userMsg = GoodsSemanticRagPrompts.buildUserMessage(query, block.toString());
        return aiLlmSupport.callText(GoodsSemanticRagPrompts.SYSTEM, userMsg);
    }

    private void upsert(Long goodsId, String chunkText, float[] vector) {
        String vectorLiteral = toVectorLiteral(vector);
        jdbcTemplate.update(
                """
                        INSERT INTO goods_search_embedding (goods_id, chunk_text, embedding, updated_at)
                        VALUES (?, ?, ?::vector, NOW())
                        ON CONFLICT (goods_id) DO UPDATE SET
                            chunk_text = EXCLUDED.chunk_text,
                            embedding = EXCLUDED.embedding,
                            updated_at = NOW()
                        """,
                goodsId, chunkText, vectorLiteral);
    }

    /** 名 + 规格 + 品牌 + 分类（空字段跳过） */
    static String buildChunkText(Map<String, Object> row) {
        return joinNonBlank(
                str(row.get("product_name")),
                str(row.get("spec_model")),
                str(row.get("brand_name")),
                str(row.get("category_name"))
        );
    }

    private static String joinNonBlank(String... parts) {
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (StringUtils.hasText(p)) {
                list.add(p.trim());
            }
        }
        return String.join(" ", list);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private void assertDimension(float[] vector) {
        if (vector == null || vector.length != embeddingDimensions) {
            int len = vector == null ? -1 : vector.length;
            throw new BusinessException(ResultCode.FAIL,
                    "向量维度不匹配：期望 " + embeddingDimensions + "，实际 " + len
                            + "。请检查 spring.ai.openai.embedding 与表 vector(" + embeddingDimensions + ")");
        }
    }

    /** 转成 pgvector 字面量：[0.1,0.2,...] */
    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.US, "%.8f", vector[i]));
        }
        sb.append(']');
        return sb.toString();
    }
}
