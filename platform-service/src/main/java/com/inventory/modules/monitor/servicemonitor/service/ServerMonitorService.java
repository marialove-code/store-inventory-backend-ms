package com.inventory.modules.monitor.servicemonitor.service;


import com.inventory.modules.monitor.servicemonitor.vo.ServerMonitorVo;

/**
 * 服务监控
 */
public interface ServerMonitorService {

    /**
     * 获取服务监控信息
     *
     * @return 服务监控数据
     */
    ServerMonitorVo getInfo();

}