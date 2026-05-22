package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.SysPermission;
import com.inventory.entity.SysRolePermission;
import com.inventory.service.SysRolePermissionService;
import com.inventory.mapper.SysRolePermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role_permission(角色权限关联表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
    implements SysRolePermissionService{


}




