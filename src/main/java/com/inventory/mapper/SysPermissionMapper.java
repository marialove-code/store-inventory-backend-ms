package com.inventory.mapper;

import com.inventory.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Mapper
* @createDate 2026-05-09 17:31:43
* @Entity com.inventory.entity.SysPermission
*/
public interface SysPermissionMapper extends BaseMapper<SysPermission> {


    /**
     * 根据用户ID查询用户拥有的所有权限标识
     */
    List<String> listPermCodesByUserId(Long userId);
}




