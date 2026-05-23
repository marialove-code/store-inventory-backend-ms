package com.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.result.Result;
import com.inventory.common.result.ResultCode;
import com.inventory.common.utils.DesensitizeUtil;
import com.inventory.constant.RedisConstants;
import com.inventory.entity.SysUser;
import com.inventory.entity.SysUserListVO;
import com.inventory.entity.SysUserRole;
import com.inventory.entity.login.LoginUserVO;
import com.inventory.mapper.SysUserMapper;
import com.inventory.mapper.SysUserRoleMapper;
import com.inventory.service.SysUserService;
import com.inventory.service.UserSessionService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户管理服务实现
 * 只负责用户CRUD、状态修改、重置密码、删除
 * 不掺杂登录、Token、Redis等认证逻辑
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    private final RedisTemplate<String, Object> redisTemplate;

    private final SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private UserSessionService userSessionService;


    @Override
    public Page<SysUserListVO> pageUser(String keyword, Integer status, Long pageNum, Long pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getIsDeleted, 0);

        // 关键词模糊查询
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUserName, keyword)
                    .or().like(SysUser::getNickName, keyword)
                    .or().like(SysUser::getPhone, keyword)
            );
        }
        // 状态筛选
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        // 按创建时间倒序
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = this.page(page, wrapper);

        // 分页VO转换
        Page<SysUserListVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());

        // ====================== ✅ 这里加上脱敏 ======================
        List<SysUserListVO> voList = userPage.getRecords().stream()
                .map(u -> {
                    SysUserListVO vo = BeanUtil.copyProperties(u, SysUserListVO.class);
                    // 手机号脱敏
                    vo.setPhone(DesensitizeUtil.mobile(vo.getPhone()));
                    return vo;
                })
                .collect(Collectors.toList());
        // ============================================================

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public SysUser getUserById(Long id) {
        SysUser user = this.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 密码置空不返回前端
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateUser(Long id, SysUser dto) {
        SysUser exist = this.getById(id);
        if (exist == null || exist.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getId, id);

        if (StrUtil.isNotBlank(dto.getNickName())) {
            wrapper.set(SysUser::getNickName, dto.getNickName());
        }
        if (dto.getSex() != null) {
            wrapper.set(SysUser::getSex, dto.getSex());
        }
        if (dto.getAge() != null) {
            wrapper.set(SysUser::getAge, dto.getAge());
        }
        if (StrUtil.isNotBlank(dto.getPhone())) {
            wrapper.set(SysUser::getPhone, dto.getPhone());
        }
        if (StrUtil.isNotBlank(dto.getEmail())) {
            wrapper.set(SysUser::getEmail, dto.getEmail());
        }
        if (StrUtil.isNotBlank(dto.getAvatar())) {
            wrapper.set(SysUser::getAvatar, dto.getAvatar());
        }
        // 有传入密码则加密更新
        if (StrUtil.isNotBlank(dto.getPassword())) {
            wrapper.set(SysUser::getPassword, passwordEncoder.encode(dto.getPassword()));
        }
        this.update(wrapper);
    }
    /**
     * 修改用户状态（禁用/启用）
     * 1. 校验用户是否存在
     * 2. 保护超级管理员
     * 3. 校验状态值是否合法
     * 4. 禁用时：强制用户下线
     * 5. 更新用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateUserStatus(Long id, Integer status) {

        // 1. 校验用户是否存在
        SysUser user = this.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 2. 超级管理员不允许禁用
        if ("super_admin".equals(user.getUserName())) {
            return Result.fail("超级管理员账号不允许修改状态");
        }

        // 3. 校验状态值是否合法
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态值不正确");
        }

        // 4. 如果是禁用操作 → 立即强制下线
        if (status == 0) {
            userSessionService.kickUserOffline(id);
        }

        // 5. 更新用户状态
        user.setStatus(status);
        this.updateById(user);

        return Result.success();
    }



    @Override
    public void resetPassword(Long id) {
        SysUser user = this.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 重置为123456
        user.setPassword(passwordEncoder.encode("123456@789"));
        this.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        SysUser user = this.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 逻辑删除
        user.setIsDeleted(1);
        this.updateById(user);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.in(SysUser::getId, ids).set(SysUser::getIsDeleted, 1);
        this.update(wrapper);
    }

    /**
     * 条件统计
     */
    @Override
    public long count(LambdaQueryWrapper<SysUser> wrapper) {
        return baseMapper.selectCount(wrapper);
    }

    /**
     * 条件查询单个
     */
    @Override
    public SysUser getOne(LambdaQueryWrapper<SysUser> wrapper) {
        return baseMapper.selectOne(wrapper);
    }


    @Override
    public List<Long> getUserRoleIds(Long userId) {
        // 构建查询条件：查询该用户的所有角色关联记录
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getUserId, userId);
        // 只查询role_id字段，避免查询多余字段
        queryWrapper.select(SysUserRole::getRoleId);

        // 查询并转换为角色ID列表
        return sysUserRoleMapper.selectList(queryWrapper)
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID删除用户（带事务、踢下线、清理关联数据）
     * 1. 校验用户是否存在
     * 2. 保护超级管理员不被删除
     * 3. 强制用户下线（清空Token + 权限缓存）
     * 4. 删除用户与角色的关联数据
     * 5. 删除用户本身
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeUserById(Long id) {

        // 1. 校验：用户是否存在
        SysUser user = this.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 2. 保护超级管理员，不允许删除
        if ("super_admin".equals(user.getUserName())) {
            return Result.fail("超级管理员账号不允许删除");
        }

        // 3. 强制用户下线：清空双Token + 权限缓存
        userSessionService.kickUserOffline(id);

        // 4. 删除用户与角色的关联数据（sys_user_role）
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));

        // 5. 删除用户本身
        this.removeById(id);
        return Result.success();
    }


    /**
     * 批量删除用户
     * 1. 校验参数合法性
     * 2. 拦截超级管理员账号删除
     * 3. 逐个清理下线缓存、关联数据
     * 4. 批量删除用户数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchRemoveUser(List<Long> idList) {
        // 校验传入ID集合
        if(CollectionUtils.isEmpty(idList)){
            return Result.fail("请选择待删除用户");
        }

        List<String> errorMsg = new ArrayList<>();
        List<Long> allowDelIds = new ArrayList<>();

        // 遍历校验每条用户数据
        for (Long userId : idList) {
            SysUser user = this.getById(userId);
            if(user == null){
                errorMsg.add("ID["+userId+"]用户不存在");
                continue;
            }
            // 禁止删除超级管理员
            if("super_admin".equals(user.getUserName())){
                errorMsg.add("超级管理员账号不可删除");
                continue;
            }
            allowDelIds.add(userId);
        }

        // 存在校验异常直接返回
        if(!errorMsg.isEmpty()){
            return Result.fail(String.join("；",errorMsg));
        }
        if(allowDelIds.isEmpty()){
            return Result.fail("无符合删除条件的用户");
        }

        // 逐个清理下线缓存、用户角色关联
        for (Long userId : allowDelIds) {
            // 强制下线清空令牌与权限缓存
            userSessionService.kickUserOffline(userId);            // 删除用户角色关联数据
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId,userId));
        }

        // 批量删除用户主体数据
        this.removeByIds(allowDelIds);
        return Result.success();
    }

    /**
     * 用户分配/解绑角色
     * 先清空原有角色关联，再新增绑定关系
     * 权限变更后清空该用户权限缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> assignUserRole(Long userId, List<Long> roleIdList) {
        // 校验用户是否存在
        SysUser user = this.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 先删除该用户所有原有角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(wrapper);

        // 批量新增角色绑定关系
        if (!CollectionUtils.isEmpty(roleIdList)) {
            List<SysUserRole> userRoleList = roleIdList.stream()
                    .map(roleId -> {
                        SysUserRole userRole = new SysUserRole();
                        userRole.setId(IdWorker.getId());
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    }).collect(Collectors.toList());
            sysUserRoleMapper.batchInsert(userRoleList);
        }

        // 角色变更，清空用户权限缓存
        userSessionService.kickUserOffline(userId);    // 删除用户角色关联数据
        return Result.success();
    }



}