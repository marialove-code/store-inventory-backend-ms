package com.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.result.Result;
import com.inventory.entity.SysPermission;
import com.inventory.entity.SysRolePermission;
import com.inventory.entity.menu.MenuVO;
import com.inventory.mapper.SysRolePermissionMapper;
import com.inventory.mapper.SysUserRoleMapper;
import com.inventory.service.SysPermissionService;
import com.inventory.mapper.SysPermissionMapper;
import com.inventory.service.UserSessionService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
    implements SysPermissionService{

     @Resource
     private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysUserRoleMapper userRoleMapper;


    @Resource
    private UserSessionService userSessionService;
    /**
     * 1. 根据用户ID查询权限标识集合（用于SpringSecurity权限校验）
     */
    @Override
    public List<String> listPermCodesByUserId(Long userId) {
        return baseMapper.listPermCodesByUserId(userId);
    }

    /**
     * 2. 构建菜单树形结构（递归）
     */
    @Override
    /**
     * 构建菜单树形结构
     * @param permissions 权限列表（只包含M目录、C菜单）
     * @return 树形菜单
     */
    public List<MenuVO> buildMenuTree(List<SysPermission> permissions) {
        // 1. 数据库实体 转 前端VO（字段严格对应你的表）
        List<MenuVO> menuList = permissions.stream()
                .map(p -> {
                    MenuVO vo = new MenuVO();
                    vo.setId(p.getId());
                    vo.setParentId(p.getParentId());
                    vo.setPermName(p.getPermName());   // 菜单名
                    vo.setPath(p.getPath());           // 路由
                    vo.setComponent(p.getComponent()); // 组件
                    vo.setIcon(p.getIcon());           // 图标
                    vo.setPermType(p.getPermType());   // M/C/F
                    vo.setPermCode(p.getPermCode());         // 权限标识（你表是 perms 字段）
                    vo.setSort(p.getSort());
                    vo.setStatus(p.getStatus());  // 状态
                    vo.setCreateTime(p.getCreateTime());  //创建时间
                    vo.setUpdateTime(p.getUpdateTime());  //更新时间
                    return vo;
                })
                .collect(Collectors.toList());

        // 2. 找出顶级菜单（parentId = 0）
        List<MenuVO> rootMenus = menuList.stream()
                .filter(menu -> menu.getParentId() == 1924700000000000000L )
                .sorted((a, b) -> a.getSort() - b.getSort())
                .collect(Collectors.toList());

        // 3. 递归设置子菜单
        for (MenuVO root : rootMenus) {
            findChildren(root, menuList);
        }

        return rootMenus;
    }


    @Override
    public List<SysPermission> listAllNormalPermissions() {
        LambdaQueryWrapper<SysPermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSort);
        List<SysPermission> sysPermissions = baseMapper.selectList(queryWrapper);
        return sysPermissions;
    }

    @Override
    public List<String> listAllPermCodes() {
        LambdaQueryWrapper<SysPermission> wrapper = Wrappers.lambdaQuery();

        // 只查询 perm_code 字段
        wrapper.select(SysPermission::getPermCode);

        // 只查询正常状态
        wrapper.eq(SysPermission::getStatus, 1);

        // 查询数据
        List<SysPermission> permissionList = baseMapper.selectList(wrapper);

        // 返回结果
        List<String> permCodes = new ArrayList<>();

        // 判空
        if (permissionList != null && !permissionList.isEmpty()) {

            for (SysPermission permission : permissionList) {

                // 防止 permission 本身为空
                if (permission == null) {
                    continue;
                }

                String permCode = permission.getPermCode();

                // 防止 permCode 为空
                if (permCode == null) {
                    continue;
                }

                // 去除空字符串
                if ("".equals(permCode.trim())) {
                    continue;
                }

                permCodes.add(permCode);
            }
        }

        return permCodes;
    }

    @Override
    public List<SysPermission> getMenuPermissionsByUserId(Long userId) {
        return baseMapper.selectMenuPermissionsByUserId(userId);
    }

    /**
     * 修改菜单状态（禁用/启用，下级同步禁用）
     * 1. 校验菜单是否存在
     * 2. 校验状态值合法性
     * 3. 禁用时：同步禁用所有子菜单，清权限缓存
     * 4. 更新状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMenuStatus(Long id, Integer status) {
        // 1. 校验菜单是否存在
        SysPermission menu = this.getById(id);
        if (menu == null) {
            return Result.fail("菜单不存在");
        }

        // 2. 校验状态值
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态值不正确");
        }

        // 3. 递归获取当前菜单及其所有子菜单ID
        List<Long> allMenuIds = new ArrayList<>();
        collectChildMenuIds(id, allMenuIds);

        // 4. 批量更新所有菜单状态
        List<SysPermission> menuList = allMenuIds.stream()
                .map(menuId -> {
                    SysPermission m = new SysPermission();
                    m.setId(menuId);
                    m.setStatus(status);
                    return m;
                }).collect(Collectors.toList());
        this.updateBatchById(menuList);

        // 5. 禁用时清空权限缓存
        if (status == 0) {
            // 查出关联了这些菜单的角色ID
            List<Long> roleIds = sysRolePermissionMapper.selectRoleIdsByMenuIds(allMenuIds);
            for (Long roleId : roleIds) {
                List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
                for (Long userId : userIds) {
                    userSessionService.kickUserOffline(userId);
                }
            }
        }

        return Result.success();
    }

    /**
     * 删除菜单（级联删除子菜单 + 清关联 + 清权限缓存）
     * 1. 校验菜单是否存在
     * 2. 递归删除所有子菜单
     * 3. 清除角色-菜单关联
     * 4. 清除菜单-权限关联
     * 5. 清空所有关联角色下用户的权限缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeMenuById(Long id) {
        // 1. 校验菜单是否存在
        SysPermission menu = this.getById(id);
        if (menu == null) {
            return Result.fail("菜单不存在");
        }

        // 2. 递归删除所有子菜单（含自身）
        List<Long> allMenuIds = new ArrayList<>();
        collectChildMenuIds(id, allMenuIds);

        // 3. 清除角色-菜单关联
        LambdaQueryWrapper<SysRolePermission> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(SysRolePermission::getPermId, allMenuIds);
        sysRolePermissionMapper.delete(roleMenuWrapper);

        // 4. 清除菜单-权限关联（如果你的设计里有这个表）
        // LambdaQueryWrapper<SysMenuPermission> menuPermWrapper = new LambdaQueryWrapper<>();
        // menuPermWrapper.in(SysMenuPermission::getMenuId, allMenuIds);
        // menuPermissionMapper.delete(menuPermWrapper);

        // 5. 删除菜单数据
        this.removeByIds(allMenuIds);

        // 6. 清空所有关联角色下用户的权限缓存
        // 先查出关联了这些菜单的角色ID
        List<Long> roleIds = sysRolePermissionMapper.selectRoleIdsByMenuIds(allMenuIds);
        for (Long roleId : roleIds) {
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
            for (Long userId : userIds) {
                userSessionService.kickUserOffline(userId);
            }
        }

        return Result.success();
    }

    /**
     * 删除权限标识
     * 1. 校验权限是否存在
     * 2. 清除角色-权限关联
     * 3. 清空所有关联角色下用户的权限缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removePermissionById(Long id) {
        // 1. 校验权限是否存在
        SysPermission permission = this.getById(id);
        if (permission == null) {
            return Result.fail("权限标识不存在");
        }

        // 2. 清除角色-权限关联
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getPermId, id);
        sysRolePermissionMapper.delete(wrapper);

        // 3. 删除权限标识
        this.removeById(id);

        // 4. 清空所有关联角色下用户的权限缓存
        List<Long> roleIds = sysRolePermissionMapper.selectRoleIdsByPermId(id);
        for (Long roleId : roleIds) {
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
            for (Long userId : userIds) {
                userSessionService.kickUserOffline(userId);
            }
        }

        return Result.success();
    }

    /**
     * 修改权限状态（禁用/启用）
     * 1. 校验权限是否存在
     * 2. 校验状态值合法性
     * 3. 禁用时：清空所有关联角色下用户的权限缓存
     * 4. 更新状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updatePermissionStatus(Long id, Integer status) {
        // 1. 校验权限是否存在
        SysPermission permission = this.getById(id);
        if (permission == null) {
            return Result.fail("权限标识不存在");
        }

        // 2. 校验状态值
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态值不正确");
        }

        // 3. 更新权限状态
        permission.setStatus(status);
        this.updateById(permission);

        // 4. 禁用时清空权限缓存
        if (status == 0) {
            List<Long> roleIds = sysRolePermissionMapper.selectRoleIdsByPermId(id);
            for (Long roleId : roleIds) {
                List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
                for (Long userId : userIds) {
                    userSessionService.kickUserOffline(userId);
                }
            }
        }

        return Result.success();
    }

    /**
     * 递归收集当前菜单及其所有子菜单ID
     */
    private void collectChildMenuIds(Long parentId, List<Long> menuIds) {
        menuIds.add(parentId);
        List<SysPermission> children = this.list(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, parentId));
        for (SysPermission child : children) {
            collectChildMenuIds(child.getId(), menuIds);
        }
    }

    /**
     * 递归查找子节点
     */
    private void findChildren(MenuVO parent, List<MenuVO> menuList) {
        // 找出当前菜单的子菜单
        List<MenuVO> children = menuList.stream()
                .filter(menu -> parent.getId().equals(menu.getParentId()))
                .sorted((a, b) -> a.getSort() - b.getSort())
                .collect(Collectors.toList());

        // 设置子菜单
        parent.setChildren(children);

        // 递归：子菜单继续找子菜单
        for (MenuVO child : children) {
            findChildren(child, menuList);
        }
    }



    // ====================== 【权限树专用：构建整棵树（包含所有权限根节点）】 ======================
    @Override
    public List<MenuVO> buildAllMenuTree(List<SysPermission> permissions) {
        // 1. 转 MenuVO
        List<MenuVO> menuList = permissions.stream()
                .map(p -> {
                    MenuVO vo = new MenuVO();
                    vo.setId(p.getId());
                    vo.setParentId(p.getParentId());
                    vo.setPermName(p.getPermName());
                    vo.setPath(p.getPath());
                    vo.setComponent(p.getComponent());
                    vo.setIcon(p.getIcon());
                    vo.setPermType(p.getPermType());
                    vo.setPermCode(p.getPermCode());
                    vo.setSort(p.getSort());
                    return vo;
                })
                .collect(Collectors.toList());

        // 2. 找根节点：parentId = -1 → 【所有权限】
        List<MenuVO> rootMenus = menuList.stream()
                .filter(menu -> menu.getParentId() == 0)
                .sorted((a, b) -> a.getSort() - b.getSort())
                .collect(Collectors.toList());

        // 3. 递归设置子节点
        for (MenuVO root : rootMenus) {
            findChildrenRecursive(root, menuList);
        }

        return rootMenus;
    }

    // 递归工具方法（加在同一个ServiceImpl里）
    private void findChildrenRecursive(MenuVO parent, List<MenuVO> allMenus) {
        List<MenuVO> children = allMenus.stream()
                .filter(menu -> parent.getId().equals(menu.getParentId()))
                .sorted((a, b) -> a.getSort() - b.getSort())
                .collect(Collectors.toList());

        parent.setChildren(children);
        for (MenuVO child : children) {
            findChildrenRecursive(child, allMenus);
        }
    }



}




