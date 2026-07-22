package com.inventory.modules.goods.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.ResultCode;
import com.inventory.modules.goods.product.entity.GoodsProduct;
import com.inventory.modules.goods.product.service.GoodsProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品 ES 核心业务（学习版）。
 * <p>
 * 依赖两个外部系统：
 * <ul>
 *   <li>{@link ElasticsearchClient}：Spring Boot 根据 spring.elasticsearch.uris 自动装配，连本机 9200</li>
 *   <li>{@link GoodsProductService}：MyBatis-Plus，读 PostgreSQL 商品表</li>
 * </ul>
 * 两条主流程：{@link #reindexFromDb()}（写索引）、{@link #search(String)}（查索引）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
public class GoodsEsSearchService {

    /** 索引名：相当于「一张专用于搜商品的表」 */
    public static final String INDEX_NAME = "goods_search";

    /** ES 官方 Java API Client；发 HTTP 到 9200（创建索引 / bulk / search） */
    private final ElasticsearchClient elasticsearchClient;

    /** 读 PG 商品；reindex 时用 */
    private final GoodsProductService goodsProductService;

    /**
     * 单次最多从库拉多少条写入 ES。
     * 配置项：app.elasticsearch.reindex-limit，默认 500。
     */
    @Value("${app.elasticsearch.reindex-limit:500}")
    private int reindexLimit;

    /**
     * 若索引不存在则创建，并声明字段类型（mapping）。
     * <p>
     * text = 会分词，适合商品名；keyword = 整词精确，适合品牌/分类。
     * 已存在则直接返回，避免重复创建报错。
     * </p>
     */
    public void ensureIndex() throws IOException {
        // 问 ES：goods_search 这个索引在不在？
        boolean exists = elasticsearchClient.indices()
                .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                .value();
        if (exists) {
            return;
        }
        // 不在：创建索引 + 指定每个字段类型
        CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(INDEX_NAME)
                .mappings(TypeMapping.of(m -> m
                        .properties("goodsId", Property.of(p -> p.long_(l -> l)))
                        .properties("productName", Property.of(p -> p.text(t -> t)))
                        .properties("specModel", Property.of(p -> p.text(t -> t)))
                        .properties("categoryName", Property.of(p -> p.keyword(k -> k)))
                        .properties("brandName", Property.of(p -> p.keyword(k -> k)))
                        .properties("shelfStatus", Property.of(p -> p.integer(i -> i)))
                ))
        );
        elasticsearchClient.indices().create(request);
        log.info("已创建 ES 索引 {}", INDEX_NAME);
    }

    /**
     * 全量同步（学习用）：PostgreSQL → Elasticsearch。
     * <p>
     * 生产常见替代：定时任务、改商品后发 MQ、Canal 听 binlog。
     * 这里故意做成「点一下接口就灌一遍」，方便演示。
     * </p>
     *
     * @return 本次尝试写入的条数（以从库查出的条数为准）
     */
    public int reindexFromDb() {
        try {
            // ① 保证索引存在
            ensureIndex();

            // ② 从 PG 查未删除商品，并限制条数（学习机别一次灌太多）
            LambdaQueryWrapper<GoodsProduct> qw = new LambdaQueryWrapper<>();
            qw.eq(GoodsProduct::getIsDeleted, 0)
                    .last("LIMIT " + Math.max(reindexLimit, 1));
            List<GoodsProduct> list = goodsProductService.list(qw);
            if (list.isEmpty()) {
                return 0;
            }

            // ③ 组装 bulk 操作：每条商品 → 一条「写入文档」指令
            List<BulkOperation> ops = new ArrayList<>(list.size());
            for (GoodsProduct p : list) {
                GoodsEsDocument doc = toDocument(p);
                // 文档 _id 用商品主键字符串；同一 id 再写入会覆盖（幂等）
                String id = String.valueOf(p.getId());
                ops.add(BulkOperation.of(b -> b.index(idx -> idx
                        .index(INDEX_NAME)
                        .id(id)
                        .document(doc)
                )));
            }

            // ④ 一次 HTTP 把多条写入 ES（比循环单条 index 快）
            BulkResponse bulk = elasticsearchClient.bulk(b -> b.operations(ops));
            if (bulk.errors()) {
                // bulk 可能部分成功；学习版只打日志，不逐条解析失败项
                log.warn("ES bulk 存在失败项，请查看 ES 日志");
            }
            log.info("ES reindex 完成 count={}", list.size());
            return list.size();
        } catch (IOException ex) {
            // 常见原因：es-dev 没起、9200 不通
            throw new BusinessException(ResultCode.FAIL,
                    "ES reindex 失败，请确认本机 9200 已启动：" + ex.getMessage());
        }
    }

    /**
     * 关键词搜索：在 productName、specModel 上做 multiMatch。
     * <p>
     * multiMatch = 同一个词同时在多个字段里找，ES 按相关度打分排序。
     * </p>
     */
    public List<GoodsEsHitVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "搜索关键词 q 不能为空");
        }
        try {
            ensureIndex();

            // 向 ES 发 search：索引名、最多返回 20 条、查询体
            SearchResponse<GoodsEsDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .size(20)
                            .query(q -> q.multiMatch(mm -> mm
                                    .query(keyword.trim())
                                    .fields("productName", "specModel")
                            )),
                    // 把每条 _source JSON 反序列化成 GoodsEsDocument
                    GoodsEsDocument.class);

            // 把 ES 的 Hit 转成对外 VO（带上 score）
            List<GoodsEsHitVO> hits = new ArrayList<>();
            for (Hit<GoodsEsDocument> hit : response.hits().hits()) {
                GoodsEsDocument src = hit.source();
                if (src == null) {
                    continue;
                }
                hits.add(GoodsEsHitVO.builder()
                        .goodsId(src.getGoodsId())
                        .productName(src.getProductName())
                        .specModel(src.getSpecModel())
                        .categoryName(src.getCategoryName())
                        .brandName(src.getBrandName())
                        .shelfStatus(src.getShelfStatus())
                        .score(hit.score())
                        .build());
            }
            return hits;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.FAIL,
                    "ES 搜索失败，请确认本机 9200 已启动：" + ex.getMessage());
        }
    }

    /**
     * 单条写入（调试用）；主流程用的是 {@link #reindexFromDb()} 的 bulk。
     */
    public void indexOne(GoodsEsDocument doc) throws IOException {
        ensureIndex();
        elasticsearchClient.index(IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .id(String.valueOf(doc.getGoodsId()))
                .document(doc)
        ));
    }

    /**
     * PG 实体 → ES 文档：只拷检索需要的字段，不把库存/价格塞进索引（学习版最小集）。
     */
    private static GoodsEsDocument toDocument(GoodsProduct p) {
        GoodsEsDocument doc = new GoodsEsDocument();
        doc.setGoodsId(p.getId());
        doc.setProductName(p.getProductName());
        doc.setSpecModel(p.getSpecModel());
        doc.setCategoryName(p.getCategoryName());
        doc.setBrandName(p.getBrandName());
        doc.setShelfStatus(p.getShelfStatus());
        return doc;
    }
}
