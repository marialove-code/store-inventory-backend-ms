package com.inventory.modules.invertory.stockin.service;

import com.inventory.common.response.Result;
import com.inventory.modules.invertory.stockin.dto.StockInAddDTO;
import com.inventory.modules.invertory.stockin.entity.InventoryIn;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 95349
* @description 针对表【inventory_in】的数据库操作Service
* @createDate 2026-05-29 19:04:18
*/
public interface InventoryInService extends IService<InventoryIn> {

    /**
     * 入库单分页查询
     * @param receiptNo 入库单号
     * @param goodsName 商品名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    Result<?> pageStockIn(String receiptNo, String goodsName, String startTime, String endTime, Long pageNum, Long pageSize);

    /**
     * 查询入库单详情
     * @param id 入库单ID
     * @return 详情
     */
    Result<?> getStockInDetail(Long id);

    /**
     * 新增入库单
     * @param dto 新增参数
     * @return 结果
     */
    Result<?> addStockIn(StockInAddDTO dto);

}
