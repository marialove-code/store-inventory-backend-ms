package com.inventory.modules.ai.semantic;

import com.inventory.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 智搜 V2：Embedding TopK + RAG 推荐说明。
 * <p>
 * 完整路径（context-path=/api）：
 * <ul>
 *   <li>POST /api/ai/goods/reindex-embedding</li>
 *   <li>GET  /api/ai/goods/semantic-search?q=续航久的手表&amp;withRag=true</li>
 * </ul>
 * </p>
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inventory.ai.semantic.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping
public class GoodsSemanticSearchController {

    private final GoodsSemanticSearchService goodsSemanticSearchService;

    /**
     * 从 PostgreSQL 商品表全量（限条）写入向量表。
     * 正式路径走网关 /api/ai/**；保留 /dev 别名便于本机 curl。
     */
    @PostMapping({"/ai/goods/reindex-embedding", "/dev/ai/goods/reindex-embedding"})
    public Result<?> reindex() {
        int count = goodsSemanticSearchService.reindexFromDb();
        Map<String, Object> data = new HashMap<>(4);
        data.put("count", count);
        data.put("message", "已从 PG 商品表 Embedding 写入 goods_search_embedding（智搜V2）");
        return Result.success(data);
    }

    /**
     * 语义检索 + RAG 推荐说明（默认 withRag=true）。
     * withRag=false 时仅返回 hits 数组，便于只测向量。
     */
    @GetMapping("/ai/goods/semantic-search")
    public Result<?> search(
            @RequestParam("q") String q,
            @RequestParam(value = "topK", required = false) Integer topK,
            @RequestParam(value = "withRag", required = false, defaultValue = "true") boolean withRag) {
        if (!withRag) {
            return Result.success(goodsSemanticSearchService.search(q, topK));
        }
        return Result.success(goodsSemanticSearchService.searchWithRag(q, topK));
    }
}
