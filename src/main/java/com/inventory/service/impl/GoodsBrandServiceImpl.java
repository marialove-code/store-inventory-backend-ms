package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.goods.GoodsBrand;
import com.inventory.service.GoodsBrandService;
import com.inventory.mapper.GoodsBrandMapper;
import org.springframework.stereotype.Service;

/**
* @author 95349
* @description 针对表【goods_brand(商品品牌表)】的数据库操作Service实现
* @createDate 2026-05-25 18:01:52
*/
@Service
public class GoodsBrandServiceImpl extends ServiceImpl<GoodsBrandMapper, GoodsBrand>
    implements GoodsBrandService{

}




