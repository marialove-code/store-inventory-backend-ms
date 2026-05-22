package com.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.Result;
import com.inventory.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.entity.menu.MenuVO;
import com.inventory.entity.menu.SysRoleListVO;

import java.util.List;

/**
* @author 95349
* @description 针对表【sys_role(系统角色表)】的数据库操作Service
* @createDate 2026-05-09 17:31:43
*/
public interface SysRoleService extends IService<SysRole> {



    /**
     * 分页查询角色数据
     * @param keyword 搜索关键词
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 分页数据
     */
    Page<SysRoleListVO> pageRole(String keyword, Integer status, Long pageNum, Long pageSize);





    /**
     * 获取系统所有权限的树形结构（用于角色分配权限）
     * 包含：目录M、菜单C、按钮F
     */
    List<MenuVO> getAllPermissionTree();

    /**
     * 查询角色已拥有的权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);

    /**
     * 保存角色与权限的关联关系
     */
    Result<Void>  saveRolePermission(Long roleId, List<Long> permIds);
    /**
     * 根据用户ID查询角色编码列表
     * @param userId 用户ID
     * @return 角色编码集合（如 SUPER_ADMIN、USER 等）
     */
    List<String> listRoleCodesByUserId(Long userId);


    /**
     * 删除角色，附带业务校验
     * @param id 角色ID
     */
    Result<Void> removeRoleById(Long id);
    /**
     * 批量删除角色，附带业务校验
     * @param ids 角色ID
     */
    Result<Void> removeRoleByIds(List<Long> ids);

    /**
     * 更新角色状态（禁用/启用）
     */
    Result<Void> updateByRoleId(Long id, Integer status);
}
