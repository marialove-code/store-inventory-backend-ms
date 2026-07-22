package com.inventory.modules.goods.es;

import lombok.Builder;
import lombok.Data;

/**
 * 返回给前端/调用方的「一条搜索命中」。
 * <p>
 * 和 {@link GoodsEsDocument} 几乎同字段，额外多一个 {@code score}（ES 相关度）。
 * 用 VO 与索引文档解耦：以后索引字段变了，接口结构可以单独调。
 * </p>
 */
@Data
@Builder
public class GoodsEsHitVO {

    /** 商品 id */
    private Long goodsId;

    /** 商品名 */
    private String productName;

    /** 规格 */
    private String specModel;

    /** 分类 */
    private String categoryName;

    /** 品牌 */
    private String brandName;

    /** 上下架 */
    private Integer shelfStatus;

    /**
     * ES 打的相关度分：越大越「像」你搜的词。
     * 列表一般已按分数从高到低排好。
     */
    private Double score;
}
