package com.inventory.service.impl.auth;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.result.ResultCode;
import com.inventory.common.utils.JwtUtil;
import com.inventory.constant.RedisConstants;
import com.inventory.context.LoginUserContext;
import com.inventory.entity.*;
import com.inventory.entity.login.LoginTokenVO;
import com.inventory.entity.login.LoginUserVO;
import com.inventory.service.SysPermissionService;
import com.inventory.service.SysRoleService;
import com.inventory.service.SysUserRoleService;
import com.inventory.service.SysUserService;
import com.inventory.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * 负责：用户登录、注册、登出、Token刷新、获取当前用户
 * 完全基于 JWT + Redis + SpringSecurity 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;
    private final SysUserRoleService sysUserRoleService;  // 注册需要绑定角色
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * AccessToken 过期时间（分钟）
     */
    @Value("${jwt.access-expire}")
    private Long accessExpire;

    /**
     * RefreshToken 过期时间（分钟）
     */
    @Value("${jwt.refresh-expire}")
    private Long refreshExpire;

    // ========================== 【新增】用户注册 ==========================
    @Override
    public void register(SysUserRegisterDTO dto) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);

        long count = sysUserService.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }

        // 2. 构建用户并加密密码
        SysUser user = new SysUser();
        user.setUserName(dto.getUserName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickName(dto.getNickName());
        user.setStatus(1);
        user.setIsDeleted(0);

        // 3. 保存用户
        sysUserService.save(user);

        // 4. 自动绑定普通用户角色（roleId=3）
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        sysUserRoleService.save(userRole);
    }

    // ========================== 登录（你原有代码） ==========================
    @Override
    public LoginTokenVO login(SysUserLoginDTO dto) {
        // ====================== 1. 查询用户（未删除 + 已启用） ======================
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);

        SysUser user = sysUserService.getOne(wrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // ====================== ✅【新增：校验用户是否被禁用】status=0 禁用 ======================
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED); // 账号已被禁用
        }
        // ====================================================================================

        // ====================== 2. 密码校验 ======================
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // ====================== 3. 查询用户角色 + 权限 ======================
        List<String> roles = sysRoleService.listRoleCodesByUserId(user.getId());
        List<String> permissions = sysPermissionService.listPermCodesByUserId(user.getId());

        // ====================== 4. 生成 JWT 双 Token ======================
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUserName());
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getUserName());

        // ====================== 5. 封装登录用户信息 ======================
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUserName());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        loginUser.setAdmin(roles.contains("SUPER_ADMIN"));

        // ====================== 6. Redis 存储 AccessToken 登录态（✅ 关键修复） ======================
        /**
         * Redis Key
         */
        String redisKey =
                RedisConstants.LOGIN_TOKEN_KEY + accessToken;

/**
 * 存储完整登录用户信息
 *
 * 后续JWT过滤器会直接从Redis获取：
 * - userId
 * - username
 * - roles
 * - permissions
 *
 * 避免每次请求查数据库
 */
        redisTemplate.opsForValue().set(
                redisKey,
                loginUser,   // ✅ 改成存 LoginUserVO
                accessExpire,
                TimeUnit.MINUTES
        );

// ====================== 7. Redis 存储 RefreshToken ======================

        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_REFRESH_KEY + refreshToken,
                user.getId(),
                refreshExpire,
                TimeUnit.MINUTES
        );

        // ====================== 8. 封装返回用户信息 ======================
        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        // ====================== 9. 封装统一返回 VO ======================
        LoginTokenVO vo = new LoginTokenVO();
        vo.setToken(accessToken);
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUser(userVO);
        // 转换并存入ThreadLocal（这里是关键修改点）


        return vo;
    }

    // ========================== 登出（你原有代码） ==========================
    @Override
    public void logout(String token, String refreshToken) {
        // ====================== 1. accessToken 判空 ======================
        if (StrUtil.isNotBlank(token)) {
            // 去掉 Bearer 前缀
            token = token.replace("Bearer ", "").trim();
            // 删除 accessToken 登录态
            redisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + token);
        }

        // ====================== 2. refreshToken 判空 ======================
        if (StrUtil.isNotBlank(refreshToken)) {
            // 删除 refreshToken 登录态
            redisTemplate.delete(RedisConstants.LOGIN_REFRESH_KEY + refreshToken);
        }

        // ====================== 3. 清空SpringSecurity上下文 ======================
        SecurityContextHolder.clearContext();
    }

    // ========================== 刷新Token（你原有代码） ==========================
    @Override
    public LoginTokenVO refreshToken(String refreshToken) {
        // ====================== 1. 校验 RefreshToken 是否为空 ======================
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // ====================== 2. 校验 RefreshToken 是否合法 ======================
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        // ====================== 3. 校验 Redis 是否存在（防止被踢下线） ======================
        Boolean hasKey = redisTemplate.hasKey(RedisConstants.LOGIN_REFRESH_KEY + refreshToken);
        if (Boolean.FALSE.equals(hasKey)) {
            throw new BusinessException(ResultCode.LOGIN_STATUS_INVALID);
        }

        // ====================== 4. 从 Token 解析用户信息 ======================
        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        // ====================== 5. 生成新的 AccessToken ======================
        String newAccessToken = jwtUtil.createAccessToken(userId, username);

        // ====================== 6. 校验用户状态是否正常 ======================
        SysUser user = sysUserService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 用户被禁用
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 用户被删除
        if (user.getIsDeleted() != 0) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // ====================== 7. 查询角色权限 ======================
        List<String> roles = sysRoleService.listRoleCodesByUserId(userId);
        List<String> permissions = sysPermissionService.listPermCodesByUserId(userId);

        // ====================== 8. 封装登录信息 ======================
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        loginUser.setAdmin(roles.contains("SUPER_ADMIN"));

        // ====================== 9. 保存新 AccessToken 到 Redis ======================
        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_TOKEN_KEY + newAccessToken,
                loginUser,
                accessExpire,
                TimeUnit.MINUTES
        );

        // ====================== 10. 封装返回用户 ======================
        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        // ====================== 11. 返回新 Token ======================
        LoginTokenVO vo = new LoginTokenVO();
        vo.setToken(newAccessToken);
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUser(userVO);

        return vo;
    }

    // ========================== 【新增】获取当前登录用户 ==========================

    /**
     * 获取当前登录用户信息
     * <p>
     * 说明：
     * 1. 前端进入系统后会调用该接口
     * 2. JWT过滤器已经完成认证
     * 3. 当前用户信息会存入 SpringSecurity 上下文
     */
    @Override
    public SysUserSimpleVO currentUser() {

        /**
         * 1. 获取认证对象
         */
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        /**
         * 2. 未登录判断
         *
         * 以下情况都视为未登录：
         * - authentication为空
         * - principal为空
         * - principal不是UserDetails类型
         */
        if (authentication == null
                || authentication.getPrincipal() == null
                || !(authentication.getPrincipal() instanceof UserDetails)) {

            throw new BusinessException(ResultCode.NOT_LOGIN);
        }

        /**
         * 3. 获取当前登录用户名
         *
         * JWT过滤器中：
         * new User(username, "", authorities)
         *
         * 所以这里可以直接获取用户名
         */
        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String username = userDetails.getUsername();

        /**
         * 4. 查询数据库用户信息
         */
        LambdaQueryWrapper<SysUser> wrapper =
                Wrappers.lambdaQuery();

        wrapper.eq(SysUser::getUserName, username);

        // 逻辑删除判断
        wrapper.eq(SysUser::getIsDeleted, 0);

        SysUser user = sysUserService.getOne(wrapper);

        /**
         * 5. 用户不存在
         */
        if (user == null) {

            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        /**
         * 6. 转换VO
         */
        SysUserSimpleVO vo =
                BeanUtil.copyProperties(user, SysUserSimpleVO.class);

        /**
         * 7. 昵称为空时
         * 默认显示用户名
         */
        if (StrUtil.isBlank(vo.getNickName())) {

            vo.setNickName(user.getUserName());
        }

        /**
         * 8. 返回当前登录用户信息
         */
        return vo;
    }
}