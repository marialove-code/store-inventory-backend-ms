package com.inventory.modules.goods.brand.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品品牌 VO
 * 前端列表 / 详情展示
 */
@Data
public class GoodsBrandVO {

    /**
     * 品牌ID
     */
    private Long  id;

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
}