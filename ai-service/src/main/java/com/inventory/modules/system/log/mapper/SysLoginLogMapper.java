package com.inventory.modules.system.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.modules.system.log.entity.SysLoginLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {



    /**
     * 查询最新一条成功登录日志
     */
    SysLoginLog selectLatestSuccessLog(@Param("userId") Long userId);

    /**
     * 查询最近N条登录日志
     */
    List<SysLoginLog> selectRecentLogs(@Param("userId") Long userId, @Param("limit") int limit);

}