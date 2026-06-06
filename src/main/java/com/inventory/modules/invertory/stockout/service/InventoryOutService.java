package com.inventory.modules.invertory.stockout.service;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockout.dto.StockOutAddDTO;
import com.inventory.modules.invertory.stockout.entity.InventoryOut;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 95349
* @description 针对表【inventory_out】的数据库操作Service
* @createDate 2026-05-29 19:04:18
*/
public interface InventoryOutService extends IService<InventoryOut> {

    /**
     * 出库单分页查询
     * @param outboundNo 出库单号
     * @param goodsName 商品名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    Result<?> pageStockOut(String outboundNo, String goodsName, String startTime, String endTime, Long pageNum, Long pageSize);



    /**
     * 新增出库单
     * @param dto 新增参数
     * @return 结果
     */
    Result<?> addStockOut(StockOutAddDTO dto);
}
