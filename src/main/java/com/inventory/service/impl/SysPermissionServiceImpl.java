package com.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.entity.SysPermission;
import com.inventory.entity.menu.MenuVO;
import com.inventory.service.SysPermissionService;
import com.inventory.mapper.SysPermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【sys_permission(系统权限表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
    implements SysPermissionService{


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
                    vo.setSort(p.getSort());           // 排序
                    return vo;
                })
                .collect(Collectors.toList());

        // 2. 找出顶级菜单（parentId = 0）
        List<MenuVO> rootMenus = menuList.stream()
                .filter(menu -> menu.getParentId() == 5)
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
                .filter(menu -> menu.getParentId() == -1)
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




