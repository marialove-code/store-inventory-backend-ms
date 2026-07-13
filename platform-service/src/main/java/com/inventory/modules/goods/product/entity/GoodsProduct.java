package com.inventory.modules.goods.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 商品信息表
 * @TableName goods_product
 */
@TableName(value ="goods_product")
@Data
public class GoodsProduct {
    /**
     * 雪花算法ID
     */
    @TableId
    private Long id;

    /**
     * 主图URL
     */
    private String mainImage;

    /**
     * 商品图片列表JSON数组
     */
    private String images;

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
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 供应商
     */
    private String supplierName;

    /**
     * 厂家
     */
    private String manufacturer;

    /**
     * 单位
     */
    private String unit;

    /**
     * 进货价/成本价
     */
    private BigDecimal costPrice;

    /**
     * 售价(标价)
     */
    private BigDecimal salePrice;


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
     * 排序号(数字越小越靠前)
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0=正常 1=已删除
     */
    private Integer isDeleted;
}