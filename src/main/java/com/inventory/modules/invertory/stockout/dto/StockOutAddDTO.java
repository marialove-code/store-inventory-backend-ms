package com.inventory.modules.invertory.stockout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增出库单请求DTO
 * 对应接口：POST /inventory/stockout
 * 核心作用：接收前端新增出库单的请求参数，并通过JSR380注解做参数合法性校验
 */
@Data
public class StockOutAddDTO {

    /**
     * 商品主键ID
     * 关联商品表 goods_product.id
     * 改为必填：保证出库单与商品的强关联，避免无商品ID的无效出库
     */
    @NotBlank(message = "商品ID不能为空")
    private String goodsId;

    /**
     * 商品名称
     * 必填，用于前端展示；与商品表名称冗余存储，提升列表查询性能
     * 冗余设计说明：避免列表查询时关联商品表，减少JOIN操作，提升分页查询效率
     */
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    /**
     * 出库数量
     * 必填，前端需校验 ≥1
     * 后端也做非空与正数校验，防止非法数据（如负数、0出库）
     */
    @NotNull(message = "出库数量不能为空")
    @Positive(message = "出库数量必须大于0")
    private Integer outboundQty;

    /**
     * 备注信息
     * 非必填，最多200字
     * 用于记录出库原因、客户信息等补充说明
     */
    @Size(max = 200, message = "备注信息不能超过200个字符")
    private String remark;
}