package com.inventory.modules.goods.brand.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 商品品牌表
 * @TableName goods_brand
 */
@TableName(value ="goods_brand")
@Data
public class GoodsBrand {
    /**
     * 雪花ID
     */
    @TableId
    private Long id;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 品牌编码
     */
    private String brandCode;

    /**
     * 品牌logo
     */
    private String logo;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态 1=启用 0=禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0=未删 1=已删
     */
    private Integer isDeleted;
}