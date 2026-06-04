package com.inventory.modules.dashboard.service;

import com.inventory.modules.dashboard.dto.DashboardIndexVO;

public interface DashboardIndexService {
    /**
     * 获取首页聚合数据
     */
    DashboardIndexVO getDashboardIndexData(String period);
}