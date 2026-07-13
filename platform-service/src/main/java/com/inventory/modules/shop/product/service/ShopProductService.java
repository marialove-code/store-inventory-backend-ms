package com.inventory.modules.shop.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.common.page.PageResult;
import com.inventory.common.response.Result;
import com.inventory.modules.shop.dashboard.vo.ShopStockWarnVO;
import com.inventory.modules.shop.product.dto.ShopProductCreateDto;
import com.inventory.modules.shop.product.dto.ShopProductUpdateDto;
import com.inventory.modules.shop.product.entity.ShopProduct;
import com.inventory.modules.shop.product.entity.ShopProductListParam;
import com.inventory.modules.shop.product.vo.ShopProductOptionVo;
import com.inventory.modules.shop.product.vo.ShopProductStatsVo;
import com.inventory.modules.shop.product.vo.ShopProductVo;

import java.util.List;

public interface ShopProductService extends IService<ShopProduct> {


    /**
     * 分页查询商品列表
     */
    Result<?> getProductPage(ShopProductListParam param);

    /**
     * 获取开单下拉选项
     */
    Result<?> getProductOptions();

    /**
     * 新增商品
     */
    Result<?> createProduct(ShopProductCreateDto dto);

    /**
     * 修改商品（全字段）
     */
    Result<?> updateProduct(Long id, ShopProductUpdateDto dto);

    /**
     * 逻辑删除商品
     */
    Result<?> deleteProduct(Long id);

    /**
     * 库存金额汇总（总进价、总售价）
     */
    Result<?> getProductStats();

    //库存监控
    List<ShopStockWarnVO> getWarnStockList();
}