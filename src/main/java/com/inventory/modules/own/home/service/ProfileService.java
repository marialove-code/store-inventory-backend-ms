package com.inventory.modules.own.home.service;

import com.inventory.modules.own.home.vo.ProfileOverviewVO;

/**
 * 个人主页 Service 接口
 */
public interface ProfileService {
    /**
     * 获取个人中心总览数据
     */
    ProfileOverviewVO getOverview();

}