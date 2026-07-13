package com.inventory.modules.goods.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品品牌 DTO
 * 前端 新增 / 编辑 提交参数
 */
@Data
public class GoodsBrandDTO {

    /**
     * 品牌名称
     */
    @NotBlank(message = "品牌名称不能为空")
    private String brandName;

    /**
     * 品牌编码
     */
    private String brandCode;

    /**
     * 品牌logo地址
     */
    private String logo;

    /**
     * 排序号
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