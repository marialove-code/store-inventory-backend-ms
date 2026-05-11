package com.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.result.ResultCode;
import com.inventory.common.utils.DesensitizeUtil;
import com.inventory.entity.SysUser;
import com.inventory.entity.SysUserListVO;
import com.inventory.mapper.SysUserMapper;
import com.inventory.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = this.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long id) {
        SysUser user = this.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 重置为123456
        user.setPassword(passwordEncoder.encode("123456"));
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
}