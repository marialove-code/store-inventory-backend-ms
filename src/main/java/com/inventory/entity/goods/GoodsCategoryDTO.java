package com.inventory.entity.goods;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品分类 DTO
 * 用于接收前端 新增/编辑 提交的参数
 */
@Data
public class GoodsCategoryDTO {

    /**
     * 上级分类ID（顶级为 0）
     */
    @NotBlank(message = "上级分类不能为空")
    private String parentId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    /**
     * 排序号（越小越靠前）
     */
    @NotNull(message = "排序号不能为空")
    private Integer sort;

    /**
     * 状态 1=启用 0=禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}