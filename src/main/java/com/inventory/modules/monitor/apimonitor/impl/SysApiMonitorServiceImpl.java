package com.inventory.modules.monitor.apimonitor.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.modules.monitor.apimonitor.entity.SysApiMonitor;
import com.inventory.modules.monitor.apimonitor.mapper.SysApiMonitorMapper;
import com.inventory.modules.monitor.apimonitor.query.ApiMonitorQuery;
import com.inventory.modules.monitor.apimonitor.service.SysApiMonitorService;
import com.inventory.modules.monitor.apimonitor.vo.ApiMonitorSummaryVo;
import com.inventory.modules.monitor.apimonitor.vo.ApiMonitorVo;
import com.inventory.modules.monitor.apimonitor.vo.ApiPerfItemVo;
import com.inventory.modules.monitor.apimonitor.vo.ApiRequestTrendVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.hutool.core.convert.Convert.toDouble;
import static cn.hutool.core.convert.Convert.toLong;

@Service
@RequiredArgsConstructor
public class SysApiMonitorServiceImpl extends ServiceImpl<SysApiMonitorMapper, SysApiMonitor>
    implements SysApiMonitorService {

    private final SysApiMonitorMapper sysApiMonitorMapper;

    @Override
    public ApiMonitorVo getApiMonitor(ApiMonitorQuery query) {

        ApiMonitorVo vo = new ApiMonitorVo();

        vo.setSummary(sysApiMonitorMapper.selectSummary(
                query.getStartTime(),
                query.getEndTime()
        ));
        List<ApiPerfItemVo> list = sysApiMonitorMapper.selectApiPerfList(query);
        fillStatus(list);
        vo.setApiList(list);
        vo.setRequestTrend(sysApiMonitorMapper.selectTrend(query));



        return vo;
    }

    private void fillStatus(List<ApiPerfItemVo> list) {
        for (ApiPerfItemVo vo : list) {

            if (vo.getFailCount() != null && vo.getFailCount() > 0) {
                vo.setStatus("ERROR");
            } else if (vo.getAvgTime() != null && vo.getAvgTime() > 1000) {
                vo.setStatus("SLOW");
            } else {
                vo.setStatus("NORMAL");
            }
        }
    }
}