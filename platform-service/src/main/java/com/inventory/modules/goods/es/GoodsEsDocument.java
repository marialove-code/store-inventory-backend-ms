package com.inventory.modules.goods.es;

import lombok.Data;

/**
 * ES 里的一条「商品检索文档」——对应倒排索引里的一篇文档。
 * <p>
 * 和 {@link com.inventory.modules.goods.product.entity.GoodsProduct} 的区别：
 * <ul>
 *   <li>GoodsProduct = PostgreSQL 真相（含价格、库存等交易字段）</li>
 *   <li>GoodsEsDocument = 只为搜索准备的副本字段（越少越好）</li>
 * </ul>
 * 下单、锁库存永远不要读这个类对应的数据当真相。
 * </p>
 */
@Data
public class GoodsEsDocument {

    /** 商品主键，与 goods_product.id 一致；搜到后可拿它回 PG 查库存 */
    private Long goodsId;

    /** 商品名称：text 类型，会分词，用于模糊/关键词搜 */
    private String productName;

    /** 规格型号：同样 text，和名称一起 multiMatch */
    private String specModel;

    /** 分类名：keyword，一般精确匹配/聚合，不走全文分词 */
    private String categoryName;

    /** 品牌名 */
    private String brandName;

    /** 上下架：1 上架 / 0 下架（检索时可过滤，本学习版暂未强制过滤） */
    private Integer shelfStatus;
}
