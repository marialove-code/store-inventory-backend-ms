package com.inventory.modules.invertory.stock.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 库存列表VO
 * 字段和前端表格一一对应
 */
@Data
public class StockListVO {
    /**
     * 库存记录主键
     */
    private Long id;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 可用库存（库存数量）
     */
    private Integer stock;

    /**
     * 锁定库存
     */
    private Integer lockStock;

    /**
     * 预警阈值（库存预警值）
     */
    private Integer stockWarn;

    /**
     * 库存状态：1-正常 2-预警 3-缺货
     */
    private Integer stockStatus;

    /**
     * 排序号，数字越小越靠前
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}