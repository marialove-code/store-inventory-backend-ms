package com.inventory.modules.system.permission.mapper;

import com.inventory.modules.system.permission.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Mapper
* @createDate 2026-05-09 17:31:43
* @Entity com.inventory.modules.system.permission.entity.SysPermission
*/
public interface SysPermissionMapper extends BaseMapper<SysPermission> {


    /**
     * 根据用户ID查询用户拥有的所有权限标识
     */
    List<String> listPermCodesByUserId(Long userId);

    /**
     * 根据用户ID获取菜单
     * @param userId
     * @return
     */
    List<SysPermission> selectMenuPermissionsByUserId(Long userId);


}




