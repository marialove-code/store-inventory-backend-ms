package com.inventory.modules.goods.es;

import com.inventory.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 演示用 HTTP 入口（Controller = 对外暴露 URL）。
 * <p>
 * 实际路径还要加 context-path：{@code /api}，所以完整地址是：
 * <ul>
 *   <li>POST /api/dev/es/goods/reindex</li>
 *   <li>GET  /api/es/goods/search?q=关键词</li>
 * </ul>
 * {@code @ConditionalOnProperty}：只有配置 {@code app.elasticsearch.enabled=true} 时才创建这个 Bean；
 * 生产关掉开关后，这两个接口根本不存在，避免误用。
 * </p>
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
@RequestMapping
public class GoodsEsController {

    /** 真正干活的服务；由 Spring 构造器注入（Lombok @RequiredArgsConstructor） */
    private final GoodsEsSearchService goodsEsSearchService;

    /**
     * 学习版「同步」：从 PostgreSQL 拉一批商品，批量写入 ES。
     * <p>
     * 浏览器地址栏只能 GET，这个接口必须用 POST（curl -X POST 或 Apifox）。
     * </p>
     */
    @PostMapping("/dev/es/goods/reindex")
    public Result<?> reindex() {
        // 1）调服务：PG → ES
        int count = goodsEsSearchService.reindexFromDb();
        // 2）组装给前端看的摘要
        Map<String, Object> data = new HashMap<>(4);
        data.put("index", GoodsEsSearchService.INDEX_NAME);
        data.put("count", count);
        data.put("message", "已从 PG 同步到 ES（学习用全量 reindex）");
        // 3）统一包装成 { code, message, data }
        return Result.success(data);
    }

    /**
     * 关键词搜索：不查 PG 列表 SQL，直接问 ES。
     *
     * @param q 查询词，例如「小米」；对应 URL 参数 ?q=
     */
    @GetMapping("/es/goods/search")
    public Result<List<GoodsEsHitVO>> search(@RequestParam("q") String q) {
        return Result.success(goodsEsSearchService.search(q));
    }
}
