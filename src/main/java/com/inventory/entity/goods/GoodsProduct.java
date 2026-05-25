package com.inventory.entity.goods;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 商品主表
 * @TableName goods_product
 */
@TableName(value ="goods_product")
@Data
public class GoodsProduct {
    /**
     * 主键ID(雪花ID)
     */
    @TableId
    private Long id;

    /**
     * 
     */
    private String productName;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 规格型号
     */
    private String specModel;

    /**
     * 单位：个/盒/箱
     */
    private String unit;

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
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 进货价
     */
    private BigDecimal costPrice;

    /**
     * 售价
     */
    private BigDecimal salePrice;

    /**
     * 当前库存
     */
    private Integer stock;

    /**
     * 库存预警值
     */
    private Integer stockWarning;

    /**
     * 橱窗位置
     */
    private String showcasePosition;

    /**
     * 商品主图
     */
    private String imageUrl;

    /**
     * 上下架状态 0=下架 1=上架
     */
    private Integer shelfStatus;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除 0=未删除 1=已删除
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}