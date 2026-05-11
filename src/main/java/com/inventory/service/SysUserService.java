package com.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inventory.entity.SysUserListVO;

import java.util.List;

/**
 * @author 95349
 * @description 针对表【sys_user(系统用户表)】的数据库操作Service // 关键：继承 IService<实体类>，MyBatis-Plus 会自动提供 save()/list() 等方法
 * @createDate 2026-05-05 15:20:47
 * * 只负责用户CRUD管理，不处理登录认证
 */
public interface SysUserService extends IService<SysUser>  {

    /**
     * 分页查询用户列表
     */
    Page<SysUserListVO> pageUser(String keyword, Integer status, Long pageNum, Long pageSize);

    /**
     * 根据ID获取用户详情
     */
    SysUser getUserById(Long id);

    /**
     * 修改用户信息
     */
    void updateUser(Long id, SysUser dto);

    /**
     * 修改用户状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 重置用户密码为123456
     */
    void resetPassword(Long id);

    /**
     * 逻辑删除单个用户
     */
    void deleteUser(Long id);

    /**
     * 批量逻辑删除用户
     */
    void batchDelete(List<Long> ids);
    /**
     * 条件统计数量（注册校验用户名重复）
     */
    long count(LambdaQueryWrapper<SysUser> wrapper);

    /**
     * 条件查询单个用户（登录、获取当前用户用）
     */
    SysUser getOne(LambdaQueryWrapper<SysUser> wrapper);
}
