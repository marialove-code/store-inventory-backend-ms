package com.inventory.mapper;

import com.inventory.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role(系统角色表)】的数据库操作Mapper
* @createDate 2026-05-09 17:31:43
* @Entity com.inventory.entity.SysRole
*/
public interface SysRoleMapper extends BaseMapper<SysRole> {


    /**
     * 根据用户ID查询角色编码列表
     * @param userId 用户ID
     * @return 角色编码集合
     */
    List<String> listRoleCodesByUserId(@Param("userId") Long userId);

}




