package com.inventory.entity.goods;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 商品分类表
 * @TableName goods_category
 */
@TableName(value ="goods_category")
@Data
public class GoodsCategory {
    /**
     * 主键ID(雪花ID)
     */
    @TableId
    private Long id;

    /**
     * 父级ID，0=顶级
     */
    private Long parentId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态 0=禁用 1=启用
     */
    private Integer status;

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