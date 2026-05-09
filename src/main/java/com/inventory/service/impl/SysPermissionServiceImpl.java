package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.SysPermission;
import com.inventory.service.SysPermissionService;
import com.inventory.mapper.SysPermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
    implements SysPermissionService{

    @Override
    public List<String> listPermCodesByUserId(Long userId) {
        return baseMapper.listPermCodesByUserId(userId);
    }

}




