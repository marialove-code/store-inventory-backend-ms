package com.inventory.modules.monitor.apimonitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.modules.monitor.apimonitor.entity.SysApiMonitor;
import com.inventory.modules.monitor.apimonitor.query.ApiMonitorQuery;
import com.inventory.modules.monitor.apimonitor.vo.ApiMonitorVo;

public interface SysApiMonitorService extends IService<SysApiMonitor> {

    ApiMonitorVo getApiMonitor(ApiMonitorQuery query);

}