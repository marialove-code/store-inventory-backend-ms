package com.inventory.modules.system.user.mapper;

import com.inventory.modules.system.user.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_user(系统用户表)】的数据库操作Mapper
* @createDate 2026-05-05 15:20:47
* @Entity com.inventory.modules.system.user.entity.SysUser
*/
public interface SysUserMapper extends BaseMapper<SysUser> {


    List<SysUser> listAll();

}




