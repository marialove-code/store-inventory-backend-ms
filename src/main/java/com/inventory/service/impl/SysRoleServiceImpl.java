package com.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.SysRole;
import com.inventory.service.SysRoleService;
import com.inventory.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{

    /**
     * 根据用户ID查询角色编码列表
     * @param userId 用户ID
     * @return 角色编码集合
     */
    @Override
    public List<String> listRoleCodesByUserId(Long userId) {
        return baseMapper.listRoleCodesByUserId(userId);
    }
}




