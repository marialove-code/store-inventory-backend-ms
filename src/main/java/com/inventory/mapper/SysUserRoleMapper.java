package com.inventory.mapper;

import com.inventory.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Mapper
* @createDate 2026-05-09 17:31:43
* @Entity com.inventory.entity.SysUserRole
*/
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {


    /**
     * 批量插入用户角色关联关系
     * @param list 用户角色关联实体列表
     * @return 插入成功的行数
     */
    int batchInsert(@Param("list") List<SysUserRole> list);

    /**
     * 根据角色ID查询绑定的所有用户ID
     * @param roleId 角色ID
     * @return 用户ID集合
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);



    // 查询某个角色下的用户数量
    Long countByRoleId(@Param("roleId") Long roleId);
}




