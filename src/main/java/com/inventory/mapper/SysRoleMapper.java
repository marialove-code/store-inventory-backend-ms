package com.inventory.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.entity.menu.SysRoleListVO;
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
     * 角色分页自定义查询
     */
    Page<SysRoleListVO> selectRolePage(IPage<SysRoleListVO> page, @Param(Constants.WRAPPER) LambdaQueryWrapper<SysRole> queryWrapper);


    /**
     * 根据用户ID查询角色编码列表
     * @param userId 用户ID
     * @return 角色编码集合
     */
    List<String> listRoleCodesByUserId(@Param("userId") Long userId);

}




