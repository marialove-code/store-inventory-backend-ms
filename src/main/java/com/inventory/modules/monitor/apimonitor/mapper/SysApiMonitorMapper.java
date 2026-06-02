package com.inventory.modules.monitor.apimonitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.monitor.apimonitor.entity.SysApiMonitor;
import com.inventory.modules.monitor.apimonitor.query.ApiMonitorQuery;
import com.inventory.modules.monitor.apimonitor.vo.ApiMonitorSummaryVo;
import com.inventory.modules.monitor.apimonitor.vo.ApiPerfItemVo;
import com.inventory.modules.monitor.apimonitor.vo.ApiRequestTrendVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysApiMonitorMapper extends BaseMapper<SysApiMonitor> {



    ApiMonitorSummaryVo selectSummary(@Param("start") String start,
                                      @Param("end") String end);


    List<ApiPerfItemVo> selectApiPerfList(ApiMonitorQuery query);

    List<ApiRequestTrendVo> selectTrend(ApiMonitorQuery query);

}