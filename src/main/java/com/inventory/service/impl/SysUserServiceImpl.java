package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.SysUser;
import com.inventory.service.SysUserService;
import com.inventory.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
* @author 95349
* @description 针对表【sys_user(系统用户表)】的数据库操作Service实现 关键：继承 ServiceImpl<Mapper, 实体类>，实现 SysUserService 接口
* @createDate 2026-05-05 15:20:47
*/

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}




