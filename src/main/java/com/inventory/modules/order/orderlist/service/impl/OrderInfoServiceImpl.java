package com.inventory.modules.order.orderlist.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.modules.order.orderlist.entity.OrderInfo;
import com.inventory.modules.order.orderlist.service.OrderInfoService;
import com.inventory.modules.order.orderlist.mapper.OrderInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author 95349
* @description 针对表【order_info】的数据库操作Service实现
* @createDate 2026-05-29 19:04:18
*/
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
    implements OrderInfoService{

}




