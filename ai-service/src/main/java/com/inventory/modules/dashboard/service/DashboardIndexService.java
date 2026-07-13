package com.inventory.modules.dashboard.service;

import com.inventory.modules.dashboard.dto.DashboardIndexVO;

/**
 * 看板聚合数据（ai-service 内精简实现，供 AI 洞察/预测复用）。
 */
public interface DashboardIndexService {

    DashboardIndexVO getDashboardIndexData(String period);
}
