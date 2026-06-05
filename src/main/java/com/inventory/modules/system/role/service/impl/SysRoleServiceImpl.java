package com.inventory.modules.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.response.Result;
import com.inventory.modules.system.permission.entity.SysPermission;
import com.inventory.modules.system.role.entity.SysRole;
import com.inventory.modules.system.permission.entity.SysRolePermission;
import com.inventory.modules.system.permission.vo.MenuVO;
import com.inventory.modules.system.permission.vo.SysRoleListVO;
import com.inventory.modules.system.permission.mapper.SysPermissionMapper;
import com.inventory.modules.system.permission.mapper.SysRolePermissionMapper;
import com.inventory.modules.system.permission.mapper.SysUserRoleMapper;
import com.inventory.modules.system.role.service.SysRoleService;
import com.inventory.modules.system.permission.service.SysPermissionService;
import com.inventory.modules.system.role.mapper.SysRoleMapper;
import com.inventory.modules.auth.service.impl.UserSessionServiceImpl;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 95349
* @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
* @createDate 2026-05-09 17:31:43
*/
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService {


    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper userRoleMapper;

    // 注入权限Service，复用你已经写好的方法
    @Resource
    private SysPermissionService permissionService;

    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private  SysPermissionMapper sysPermissionMapper;
    @Resource
    private UserSessionServiceImpl userSessionService;

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
    public Result<Void> saveRolePermission(Long roleId, List<Long> permIds) {

        // ===================== 1. 业务校验 =====================
        SysRole role = this.getById(roleId);
        if (role == null) {
            return Result.fail("角色不存在");
        }

        // 超级管理员不允许修改权限
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            return Result.fail("超级管理员角色权限不可修改");
        }

        // ===================== 2. 删除该角色所有旧权限 =====================
        LambdaQueryWrapper<SysRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRolePermission :: getRoleId, roleId);
        rolePermissionMapper.delete(deleteWrapper);

        // ===================== 3. 批量插入新权限 =====================
        if (!CollectionUtils.isEmpty(permIds)) {
            List<SysRolePermission> rolePermList = permIds.stream()
                    .map(permId -> {
                        SysRolePermission rolePerm = new SysRolePermission();
                        rolePerm.setId(IdWorker.getId()); // 你的雪花ID
                        rolePerm.setRoleId(roleId);
                        rolePerm.setPermId(permId);
                        return rolePerm;
                    })
                    .collect(Collectors.toList());

            rolePermissionMapper.batchInsert(rolePermList);
        }

        // ===================== 4. 权限已修改 → 清理关联用户权限缓存 =====================
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            // 清空权限缓存（必须加！）
            userSessionService.kickUserOffline(userId);
        }

        return Result.success();
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

    @Override
    @Transactional(rollbackFor = Exception.class)  // 👈 加这个
    public Result<Void> removeRoleById(String id) {
        String msg = null;
        SysRole role = this.getById(id);

        if (role == null) {
            msg = "角色不存在";
        } else if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            msg = "超级管理员角色不允许删除";
        } else if (userRoleMapper.countByRoleId(Long.valueOf(id)) > 0) {
            msg = "该角色下存在关联用户，无法删除";
        }

        if (msg != null) {
            return Result.fail(msg);
        }

        // 查询关联用户并下线
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(Long.valueOf(id));
        batchKickOffline(userIds);
        // 删除关联数据与角色
        rolePermissionMapper.deleteByRoleId(Long.valueOf(id));
        this.removeById(id);

        return Result.success();

    }


    /**
     * 批量踢下线，清空双token与权限缓存
     */
    private void batchKickOffline(List<Long> userIdList) {
        for (Long userId : userIdList) {
            userSessionService.kickUserOffline(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务：批量操作必须加
    public Result<Void> removeRoleByIds(List<Long> ids) {
        // 1. 空集合校验
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的角色");
        }

        // 2. 批量校验：所有角色必须 【不是超级管理员】 + 【无关联用户】
        List<Long> allowDeleteIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (Long roleId : ids) {
            SysRole role = this.getById(roleId);

            // 角色不存在
            if (role == null) {
                errorMessages.add("角色ID[" + roleId + "]不存在");
                continue;
            }

            // 超级管理员不允许删
            if ("SUPER_ADMIN".equals(role.getRoleCode())) {
                errorMessages.add("超级管理员角色不允许删除");
                continue;
            }

            // 有关联用户不允许删
            long userCount = userRoleMapper.countByRoleId(roleId);
            if (userCount > 0) {
                errorMessages.add("角色[" + role.getRoleName() + "]下存在用户，无法删除");
                continue;
            }

            // 校验通过，可以删除
            allowDeleteIds.add(roleId);
        }

        // 3. 如果有错误信息，直接返回给前端（不执行任何删除）
        if (!errorMessages.isEmpty()) {
            return Result.fail(String.join("，", errorMessages));
        }

        // 4. 没有可删除的数据
        if (allowDeleteIds.isEmpty()) {
            return Result.fail("未选择可删除的角色");
        }

        // ================= 正式删除 =================
        for (Long roleId : allowDeleteIds) {
            // 查询该角色下的所有用户 → 踢下线（清空token+缓存）
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
            batchKickOffline(userIds);
            // 删除关联权限
            rolePermissionMapper.deleteByRoleId(roleId);
            // 4. 删除角色（用 this 调用父类方法，完全正确）
            this.removeById(roleId);
        }

        // 5. 批量删除角色
        this.removeByIds(allowDeleteIds);

        // 6. 返回成功
        return Result.success();
    }


    @Override
    @Transactional(rollbackFor = Exception.class) // 事务
    public Result<Void> updateByRoleId(String id, Integer status) {
        String msg = null;

        // 1. 查询角色
        SysRole role = this.getById(id);

        // 2. 校验
        if (role == null) {
            msg = "角色不存在";
        }
        // 超级管理员不能禁用
        else if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            msg = "超级管理员角色不允许修改状态";
        }
        // 状态值合法性校验
        else if (status == null || (status != 0 && status != 1)) {
            msg = "状态值不正确";
        }

        // 3. 有错误直接返回
        if (msg != null) {
            return Result.fail(msg);
        }

        // ================ 执行业务 ================
        // 如果是【禁用】操作 → 踢该角色下所有用户下线
        if (status == 0) {
            // 获取角色下所有用户ID
            List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(Long.valueOf(id));
            // 批量清空token + 权限缓存
            batchKickOffline(userIds);
        }

        // 更新状态
        role.setStatus(status);
        this.updateById(role);
        // 统一返回
        return Result.success();
    }


}




