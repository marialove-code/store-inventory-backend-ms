package com.inventory.modules.own.home.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.own.home.service.ProfileService;
import com.inventory.modules.own.home.vo.*;
import com.inventory.modules.system.log.entity.SysLoginLog;
import com.inventory.modules.system.log.mapper.SysLoginLogMapper;
import com.inventory.modules.system.permission.entity.SysPermission;
import com.inventory.modules.system.permission.entity.SysUserRole;
import com.inventory.modules.system.permission.mapper.SysPermissionMapper;
import com.inventory.modules.system.permission.mapper.SysUserRoleMapper;
import com.inventory.modules.system.role.entity.SysRole;
import com.inventory.modules.system.role.mapper.SysRoleMapper;
import com.inventory.modules.system.user.entity.SysUser;
import com.inventory.modules.system.user.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysLoginLogMapper sysLoginLogMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 核心：获取个人中心总览数据
     */
    @Override
    public ProfileOverviewVO getOverview() {
        // 1. 获取当前登录用户ID（从token/Security上下文拿）
        Long userId = LoginUserContext.getUserId();

        // 2. 查询用户基础信息（SysUser）
        SysUser user = sysUserMapper.selectById(userId);

        // 3. 查询【最新一条登录日志】（取IP、时间、地址）
        SysLoginLog lastLoginLog = sysLoginLogMapper.selectLatestSuccessLog(userId);

        // 4. 封装基础信息VO
        ProfileBasicInfoVO basic = new ProfileBasicInfoVO();
        basic.setId(user.getId());
        basic.setUserName(user.getUserName());
        basic.setNickName(user.getNickName());
        basic.setAvatar(user.getAvatar());

        basic.setAge(user.getAge());
        basic.setEmail(user.getEmail());
        basic.setPhone(user.getPhone());
        basic.setStatus(user.getStatus());
        basic.setCreateTime(user.getCreateTime());
        // 性别转换 0=未知 1=男 2=女
        if (user.getSex() == null) {
            basic.setSex("未知");
        } else {
            switch (user.getSex()){
                case 1: basic.setSex("M"); break; // 前端渲染♂
                case 2: basic.setSex("F"); break; // 前端渲染♀
                default: basic.setSex("未知");
            }
        }

        List<String> roleNameList = getRoleNames(userId);
        basic.setRoleName(roleNameList.isEmpty() ? "无" : String.join("，", roleNameList));
       // 赋值最后登录时间
        if(lastLoginLog != null){
            basic.setLastLoginTime(lastLoginLog.getLoginTime());
        }
        // 5. 封装安全信息VO（从登录日志取！）
        ProfileSecurityInfoVO security = new ProfileSecurityInfoVO();
        security.setOnlineDeviceCount(getOnlineDeviceCount(userId));
        if (lastLoginLog != null) {
            security.setLastLoginIp(lastLoginLog.getLoginIp());
            security.setLastLoginTime(lastLoginLog.getLoginTime());
            security.setLastLoginAddress(lastLoginLog.getLoginAddress());
        }
        security.setTokenStatus("有效");

        // 6. 封装权限信息
        ProfilePermissionInfoVO permission = new ProfilePermissionInfoVO();
        permission.setRoleNames(getRoleNames(userId));
        permission.setPermissionTotal(getPermissionCount(userId));
        permission.setMenuTree(buildMenuTree(userId));

        // 7. 最近3条登录日志
        List<SysLoginLog> recentLogs = sysLoginLogMapper.selectRecentLogs(userId, 3);

        // 8. 组装返回
        ProfileOverviewVO vo = new ProfileOverviewVO();
        vo.setBasicInfo(basic);
        vo.setSecurityInfo(security);
        vo.setPermissionInfo(permission);
        vo.setRecentLoginLogs(recentLogs);

        return vo;
    }



    // ===================== 工具方法 =====================

    /**
     * 获取在线设备数量
     */
    private Integer getOnlineDeviceCount(Long userId) {
        String key = "online:user:" + userId;
        Long size = redisTemplate.opsForHash().size(key);
        return size == null ? 0 : size.intValue();
    }

    /**
     * 获取角色名称列表
     */
    private List<String> getRoleNames(Long userId) {
        // 1. 构造条件：查询当前用户的角色关联关系
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getUserId, userId);

        // 2. MP 原生方法查询
        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(queryWrapper);

        // 空值判断
        if (CollUtil.isEmpty(userRoleList)) {
            return new ArrayList<>();
        }

        // 3. 提取角色 ID
        List<Long> roleIds = userRoleList.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 4. 批量查询角色
        List<SysRole> sysRoleList = sysRoleMapper.selectBatchIds(roleIds);

        // 5. 提取角色名称
        return sysRoleList.stream()
                .map(SysRole::getRoleName)
                .collect(Collectors.toList());
    }

    /**
     * 获取权限数量
     */
    private Integer getPermissionCount(Long userId) {
        Integer count = 0;
        LoginUserVO user = LoginUserContext.getUser();
        List<String> permissions = user.getPermissions();
        if(permissions.contains("SUPER_ADMIN")){
            count  = sysPermissionMapper.selectCountByUserId(userId);
        }else{
            count =  sysPermissionMapper.selectPermissionCount();

        }
        //判断当前用户如果是超级管理员 则返回所有的菜单
        return count;

    }

    /**
     * 构建菜单树（递归树形结构，父节点+子节点）
     */
    private List<MenuTreeVO> buildMenuTree(Long userId) {
        // 1. 查询当前用户的所有权限/菜单
        List<SysPermission> permissionList = sysPermissionMapper.selectListByUserId(userId);
        if (CollUtil.isEmpty(permissionList)) {
            return new ArrayList<>();
        }

        // 2. 转成 MenuTreeVO 列表
        List<MenuTreeVO> voList = permissionList.stream().map(p -> {
            MenuTreeVO vo = new MenuTreeVO();
            vo.setId(p.getId());
            vo.setPermName(p.getPermName());
            vo.setParentId(p.getParentId()); // 必须加 parentId 才能构建树
            vo.setChildren(new ArrayList<>());
            return vo;
        }).collect(Collectors.toList());

        // 3. 递归构建树形结构（只返回顶层节点 parentId = 0）
        return buildTree(voList, 0L);
    }

    /**
     * 递归工具方法：构建菜单树
     * @param list 所有菜单VO
     * @param parentId 父节点ID（顶层传 0）
     * @return 树形结构菜单
     */
    private List<MenuTreeVO> buildTree(List<MenuTreeVO> list, Long parentId) {
        List<MenuTreeVO> tree = new ArrayList<>();
        for (MenuTreeVO vo : list) {
            // 当前节点的父ID == 目标父ID → 属于这一层
            if (parentId.equals(vo.getParentId())) {
                // 递归找子节点
                List<MenuTreeVO> children = buildTree(list, vo.getId());
                vo.setChildren(children);
                tree.add(vo);
            }
        }
        return tree;
    }
}