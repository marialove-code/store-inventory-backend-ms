package com.inventory.modules.goods.category.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 商品分类表
 * @TableName goods_category
 */
@TableName(value ="goods_category")
@Data
public class GoodsCategory {
    /**
     * 雪花ID
     */
    @TableId
    private Long id;

    /**
     * 上级分类ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
    private String categoryName;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0=未删 1=已删
     */
    private Integer isDeleted;
}