package com.inventory.service;

import com.inventory.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role(系统角色表)】的数据库操作Service
* @createDate 2026-05-09 17:31:43
*/
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户ID查询角色编码列表
     * @param userId 用户ID
     * @return 角色编码集合（如 SUPER_ADMIN、USER 等）
     */
    List<String> listRoleCodesByUserId(Long userId);
}
