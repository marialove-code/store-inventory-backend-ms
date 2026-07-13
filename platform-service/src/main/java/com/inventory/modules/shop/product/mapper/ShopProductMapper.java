package com.inventory.modules.shop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.shop.dashboard.vo.ShopStockWarnVO;
import com.inventory.modules.shop.product.entity.ShopProduct;
import com.inventory.modules.shop.product.vo.ShopProductStatsVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShopProductMapper extends BaseMapper<ShopProduct> {

    /**
     * 查询库存不足预警商品（stock < stock_warn）
     */
    List<ShopStockWarnVO> selectWarnProduct();

    /**
     * 库存金额汇总：总进价、总售价
     */
    ShopProductStatsVo selectProductInventoryStats();
}