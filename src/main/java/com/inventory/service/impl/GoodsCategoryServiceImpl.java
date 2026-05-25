package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.goods.GoodsCategory;
import com.inventory.service.GoodsCategoryService;
import com.inventory.mapper.GoodsCategoryMapper;
import org.springframework.stereotype.Service;

/**
* @author 95349
* @description 针对表【goods_category(商品分类表)】的数据库操作Service实现
* @createDate 2026-05-25 18:01:52
*/
@Service
public class GoodsCategoryServiceImpl extends ServiceImpl<GoodsCategoryMapper, GoodsCategory>
    implements GoodsCategoryService{

}




