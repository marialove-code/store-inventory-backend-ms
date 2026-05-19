package com.inventory.mapper;

import com.inventory.entity.SysPermission;
import com.inventory.entity.SysRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role_permission(角色权限关联表)】的数据库操作Mapper
* @createDate 2026-05-09 17:31:43
* @Entity com.inventory.entity.SysRolePermission
*/
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    List<SysPermission> listUserPermissionsByUserId(@Param("userId") Long userId);



    /**
     * 批量插入角色权限关联关系
     * @param list 角色权限关联实体列表
     * @return 插入成功的行数
     */
    int batchInsert(@Param("list") List<SysRolePermission> list);



}




