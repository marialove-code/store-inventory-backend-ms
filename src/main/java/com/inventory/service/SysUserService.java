package com.inventory.service;

import com.inventory.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 95349
* @description 针对表【sys_user(系统用户表)】的数据库操作Service // 关键：继承 IService<实体类>，MyBatis-Plus 会自动提供 save()/list() 等方法
* @createDate 2026-05-05 15:20:47
*/

public interface SysUserService extends IService<SysUser> {
}
