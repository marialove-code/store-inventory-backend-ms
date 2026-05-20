package com.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.context.LoginUserContext;
import com.inventory.entity.SysPermission;
import com.inventory.entity.SysRole;
import com.inventory.entity.SysRolePermission;
import com.inventory.entity.login.LoginUserVO;
import com.inventory.entity.menu.MenuVO;
import com.inventory.entity.menu.SysRoleListVO;
import com.inventory.mapper.SysPermissionMapper;
import com.inventory.mapper.SysRolePermissionMapper;
import com.inventory.service.SysPermissionService;
import com.inventory.service.SysRoleService;
import com.inventory.mapper.SysRoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{

    @Resource
    private SysRoleMapper sysRoleMapper;

    // 注入权限Service，复用你已经写好的方法
    @Resource
    private SysPermissionService permissionService;

    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private  SysPermissionMapper sysPermissionMapper;

    /**
     * 角色分页条件查询
     * 匹配关键词：角色名称、角色编码模糊查询
     * 支持状态精准过滤
     */
    @Override
    public Page<SysRoleListVO> pageRole(String keyword, Integer status, Long pageNum, Long pageSize) {
        // 1. 构建分页对象
        Page<SysRoleListVO> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();

        // 状态条件不为空则拼接状态查询
        if (status != null) {
            queryWrapper.eq(SysRole::getStatus, status);
        }

        // 关键词不为空 模糊匹配角色名称 / 角色编码
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(SysRole::getRoleName, keyword)
                    .or()
                    .like(SysRole::getRoleCode, keyword);
        }

        // 3. 分页查询并自动映射为VO
        return sysRoleMapper.selectRolePage(page, queryWrapper);
    }

    /**
     * 获取所有权限树形结构
     * 直接复用权限Service已经写好的两个方法，不用重复写递归
     */
    @Override
    public List<MenuVO> getAllPermissionTree() {
        // 1. 查询所有正常的权限（包含目录、菜单、按钮）
        List<SysPermission> allPermissions = permissionService.listAllNormalPermissions();
        // 2. 直接复用你已经写好的 buildMenuTree 方法构建树形结构
        return permissionService.buildAllMenuTree(allPermissions);
    }

    /**
     * 查询角色已拥有的权限ID
     */
    @Override
    public List<Long> getRolePermissionIds(Long roleId) {


        SysRole sysRole = sysRoleMapper.selectById(roleId);
        // 3. 如果是超级管理员 → 返回所有权限ID
        if ("SUPER_ADMIN".equals(sysRole.getRoleCode())) {
            // 超级管理员直接查询全部权限，不限制角色
            return sysPermissionMapper.selectList(null)
                    .stream()
                    .map(SysPermission::getId)
                    .collect(Collectors.toList());
        }

        // 4. 普通角色 → 按角色ID查询
        LambdaQueryWrapper<SysRolePermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRolePermission::getRoleId, roleId);
        return rolePermissionMapper.selectList(queryWrapper)
                .stream()
                .map(SysRolePermission::getPermId)
                .collect(Collectors.toList());
    }

    /**
     * 保存角色权限（先删后插）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermission(Long roleId, List<Long> permIds) {
        // 1. 删除该角色所有旧权限
        LambdaQueryWrapper<SysRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(deleteWrapper);

        // 2. 批量插入新权限
        if (!CollectionUtils.isEmpty(permIds)) {
            List<SysRolePermission> rolePermList = permIds.stream()
                    .map(permId -> {
                        SysRolePermission rolePerm = new SysRolePermission();
                        // 同样手动生成雪花ID
                        rolePerm.setId(IdWorker.getId());
                        rolePerm.setRoleId(roleId);
                        rolePerm.setPermId(permId);
                        return rolePerm;
                    })
                    .collect(Collectors.toList());

            rolePermissionMapper.batchInsert(rolePermList);
        }
    }

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




