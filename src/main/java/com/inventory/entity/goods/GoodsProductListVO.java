package com.inventory.entity.goods;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表 VO（最新版 → 对齐PG表结构）
 */
@Data
public class GoodsProductListVO {

    /** 主键ID */
    private Long id;

    /** 商品主图 */
    private String mainImage;

    /** 商品名称 */
    private String productName;

    /** 规格型号 */
    private String specModel;

    /** 商品编码 */
    private String productCode;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 品牌ID */
    private Long brandId;

    /** 品牌名称 */
    private String brandName;

    /** 供应商 */
    private String supplierName;

    /** 生产厂家 */
    private String manufacturer;

    /** 单位 */
    private String unit;

    /** 进货价（成本价） */
    private BigDecimal costPrice;

    /** 销售价 */
    private BigDecimal salePrice;

    /** 实际售价（实销价） → 新增 */
    private BigDecimal actualSalePrice;

    /** 当前库存 */
    private Integer stock;

    /** 库存预警值 */
    private Integer stockWarn;

    /** 橱窗位置 */
    private String showcasePosition;

    /** 上下架状态 0=下架 1=上架 */
    private Integer shelfStatus;

    /** 排序号 → 新增 */
    private Integer sort;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}