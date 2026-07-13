package com.inventory.modules.goods.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 DTO
 * 完全按照你文字版字段生成，无多余字段
 */
@Data
public class GoodsProductDTO {

    /**
     * 雪花ID（新增不传，编辑必传）
     */
    private String id;

    /**
     * 商品主图
     */
    private String mainImage;

    /**
     * 商品多图列表
     */
    private List<String> images;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 规格型号
     */
    private String specModel;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 分类ID
     */
    private String categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 品牌ID
     */
    private String brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 供应商
     */
    private String supplierName;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 单位
     */
    private String unit;

    /**
     * 成本价/进货价
     */
    private BigDecimal costPrice;

    /**
     * 售价（必填）
     */
    private BigDecimal salePrice;

    /**
     * 实际售价/实销价
     */
    private BigDecimal actualSalePrice;

    /**
     * 当前库存
     */
    private Integer stock;

    /**
     * 库存预警值
     */
    private Integer stockWarn;

    /**
     * 橱窗位置
     */
    private String showcasePosition;

    /**
     * 上下架状态 1=上架 0=下架
     */
    private Integer shelfStatus;

    /**
     * 排序号
     */
    private Integer sort;
}