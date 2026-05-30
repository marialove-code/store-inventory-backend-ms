package com.inventory.modules.invertory.stockin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 新增入库单请求DTO
 * 对应接口：POST /inventory/stockin
 */
@Data
public class StockInAddDTO {

    /**
     * 商品主键ID
     * 关联商品表 goods_product.id
     * 建议必填，用于后续关联查询与库存更新
     */
    private String goodsId;

    /**
     * 商品名称
     * 必填，用于前端展示；与商品表名称冗余存储，提升列表查询性能
     */
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    /**
     * 入库数量
     * 必填，前端需校验 ≥1
     * 后端也做非空与正数校验，防止非法数据
     */
    @NotNull(message = "入库数量不能为空")
    @Positive(message = "入库数量必须大于0")
    private Integer receiptQty;

    /**
     * 备注信息
     * 非必填，最多200字
     * 用于记录入库原因、供应商等补充说明
     */
    @Size(max = 200, message = "备注信息不能超过200个字符")
    private String remark;
}