package com.inventory.modules.goods.category.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类 VO
 * 用于返回给前端展示（列表 + 树结构）
 */
@Data
public class GoodsCategoryVO {

    /**
     * 分类ID
     */
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
     * 排序
     */
    private Integer sort;

    /**
     * 状态 1启用 0禁用
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
     * 子分类（树结构专用）
     */
    private List<GoodsCategoryVO> children;
}