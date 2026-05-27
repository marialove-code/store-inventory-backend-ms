package com.inventory.modules.goods.brand.service;

import com.inventory.common.response.Result;
import com.inventory.modules.goods.brand.entity.GoodsBrand;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.modules.goods.brand.dto.GoodsBrandDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author 95349
* @description 针对表【goods_brand(商品品牌表)】的数据库操作Service
* @createDate 2026-05-26 19:13:00
*/
public interface GoodsBrandService extends IService<GoodsBrand> {

    /**
     * 分页查询品牌列表
     */
    Result<?> page(String keyword, Long pageNum, Long pageSize);

    /**
     * 查询全部品牌（下拉用）
     */
    Result<?> listAll();

    /**
     * 新增品牌
     */
    Result<?> add(GoodsBrandDTO dto);

    /**
     * 修改品牌
     */
    Result<?> update(String id, GoodsBrandDTO dto);

    /**
     * 删除品牌
     */
    Result<?> delete(String id);

    /**
     * 批量删除
     */
    Result<?> batchDelete(List<String> ids);

    /**
     * 修改状态
     */
    Result<?> updateStatus(String id, Integer status);

    /**
     * 上传logo
     */
    Result<?> uploadLogo(MultipartFile file);
}
