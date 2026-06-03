package com.inventory.modules.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import com.inventory.framework.security.jwt.JwtUtil;
import com.inventory.common.constants.RedisConstants;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginTokenVO;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.system.log.entity.SysLoginLog;
import com.inventory.modules.system.log.service.IpLocationService;
import com.inventory.modules.system.log.service.SysLoginLogService;
import com.inventory.modules.system.permission.entity.SysUserRole;
import com.inventory.modules.auth.dto.SysUserLoginDTO;
import com.inventory.modules.auth.dto.SysUserRegisterDTO;
import com.inventory.modules.system.user.entity.SysUser;
import com.inventory.modules.system.user.vo.SysUserSimpleVO;
import com.inventory.modules.system.permission.service.SysPermissionService;
import com.inventory.modules.system.role.service.SysRoleService;
import com.inventory.modules.system.permission.service.SysUserRoleService;
import com.inventory.modules.system.user.service.SysUserService;
import com.inventory.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.inventory.common.constants.PermissionConstants.SUPER_PERM_CODE;

/**
 * 认证服务实现类
 * 负责：用户登录、注册、登出、Token刷新、获取当前用户
 * 完全基于 JWT + Redis + SpringSecurity 实现
 */
@Slf4j
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
    private final SysLoginLogService loginLogService;
    private final IpLocationService ipLocationService;

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
    public LoginTokenVO login(SysUserLoginDTO dto, HttpServletRequest request) {
        // ====================== 1. 查询用户（未删除 + 已启用） ======================
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);

        SysUser user = sysUserService.getOne(wrapper);
        if (user == null) {
            // 【新增：记录登录失败日志】
            recordLoginLog(null, dto.getUserName(), null, request, 0, "用户不存在");
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // ====================== 2. 校验用户是否被禁用 ======================
        if (user.getStatus() == 0) {
            // 【新增：记录登录失败日志】
            recordLoginLog(user.getId(), user.getUserName(), user.getNickName(), request, 0, "账号已被禁用");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // ====================== 3. 密码校验 ======================
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            // 【新增：记录登录失败日志】
            recordLoginLog(user.getId(), user.getUserName(), user.getNickName(), request, 0, "密码错误");
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // ====================== 4. 查询用户角色 + 权限 ======================
        List<String> roles = sysRoleService.listRoleCodesByUserId(user.getId());
        boolean isSuperAdmin = roles.contains("SUPER_ADMIN");

        List<String> permissions;
        if (isSuperAdmin) {
            permissions = sysPermissionService.listAllPermCodes();
        } else {
            List<String> rawPermissions = sysPermissionService.listPermCodesByUserId(user.getId());
            permissions = new ArrayList<>();
            if (rawPermissions != null && !rawPermissions.isEmpty()) {
                for (String code : rawPermissions) {
                    if (!SUPER_PERM_CODE.equals(code)) {
                        permissions.add(code);
                    }
                }
            }
        }

        // ====================== 5. 生成 JWT 双 Token ======================
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUserName());
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getUserName());

        // ====================== 6. 封装登录用户信息 ======================
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickName(user.getNickName());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        loginUser.setAdmin(isSuperAdmin);

        // ========================== 【原有代码不变】 ==========================
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        cn.hutool.http.useragent.UserAgent ua = cn.hutool.http.useragent.UserAgentUtil.parse(userAgent);
        String browser = ua.getBrowser().getName();
        String os = ua.getOs().getName();

        LocalDateTime loginTime = LocalDateTime.now();
        LocalDateTime expireTime = loginTime.plusMinutes(accessExpire);

        loginUser.setIpaddr(ip);
        loginUser.setBrowser(browser);
        loginUser.setOs(os);
        loginUser.setLoginTime(loginTime);
        loginUser.setExpireTime(expireTime);
        // ========================== 【原有代码结束】 ==========================

        // 【新增：记录登录成功日志】
        recordLoginLog(user.getId(), user.getUserName(), user.getNickName(), request, 1, "登录成功");

        // ====================== 7. Redis 存储 AccessToken 登录态 ======================
        String redisAccessKey = "user:token:" + user.getId() + ":access:" + accessToken;
        redisTemplate.opsForValue().set(redisAccessKey, loginUser, accessExpire, TimeUnit.MINUTES);

        // ====================== 8. Redis 存储 RefreshToken ======================
        String redisRefreshKey = "user:token:" + user.getId() + ":refresh:" + refreshToken;
        redisTemplate.opsForValue().set(redisRefreshKey, user.getId(), refreshExpire, TimeUnit.MINUTES);

        // ====================== 9. Redis 单独存储用户权限缓存 ======================
        String redisPermKey = "user:perm:" + user.getId();
        redisTemplate.opsForValue().set(redisPermKey, permissions, accessExpire, TimeUnit.MINUTES);

        // ====================== 10. 封装返回给前端 ======================
        SysUserSimpleVO userVO = BeanUtil.copyProperties(loginUser, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        LoginTokenVO vo = new LoginTokenVO();
        vo.setToken(accessToken);
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUser(userVO);

        return vo;
    }

    /**
     * 踢用户下线：删除 Redis 中所有 AccessToken、RefreshToken 和权限缓存
     *
     * @param userId 用户ID
     */
    private void kickUserOffline(Long userId) {
        // ----------------- 1. 删除该用户所有 AccessToken -----------------
        Set<String> accessKeys = redisTemplate.keys("user:token:" + userId + ":access:*");
        if (accessKeys != null && !accessKeys.isEmpty()) {
            redisTemplate.delete(accessKeys);
        }

        // ----------------- 2. 删除该用户所有 RefreshToken -----------------
        Set<String> refreshKeys = redisTemplate.keys("user:token:" + userId + ":refresh:*");
        if (refreshKeys != null && !refreshKeys.isEmpty()) {
            redisTemplate.delete(refreshKeys);
        }

        // ----------------- 3. 删除该用户权限缓存 -----------------
        String permKey = "user:perm:" + userId;
        redisTemplate.delete(permKey);
    }

    // ========================== 登出（你原有代码） ==========================
    @Override
    public void logout(String token, String refreshToken) {

        // ====================== accessToken ======================
        if (StrUtil.isNotBlank(token)) {

            token = token.trim();

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            redisTemplate.delete(
                    RedisConstants.LOGIN_ACCESS_PREFIX + token
            );
        }

        // ====================== refreshToken ======================
        if (StrUtil.isNotBlank(refreshToken)) {

            refreshToken = refreshToken.trim();

            redisTemplate.delete(
                    RedisConstants.LOGIN_REFRESH_PREFIX + refreshToken
            );
        }

        // ====================== 清空认证上下文 ======================
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
        Boolean hasKey = redisTemplate.hasKey(RedisConstants.LOGIN_REFRESH_PREFIX + refreshToken);
        if (Boolean.FALSE.equals(hasKey)) {
            throw new BusinessException(ResultCode.LOGIN_STATUS_INVALID);
        }

        // ====================== 4. 从 Token 解析用户信息 ======================
        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        // ====================== 5. 生成新的 AccessToken ======================
        String newAccessToken = jwtUtil.createAccessToken(userId, username);

        // ====================== 6. 校验用户状态是否正常 ======================
        SysUser user = sysUserService.getUserById(String.valueOf(userId));
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
                RedisConstants.LOGIN_ACCESS_PREFIX + newAccessToken,
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
    public Result<SysUserSimpleVO> currentUser() {

        // ====================== 1. 从 LoginUserContext 获取登录信息 ======================
        LoginUserVO loginUser = LoginUserContext.getUser();
        if (loginUser == null) {
            // 理论上不会发生，因为 JWT 过滤器已经鉴权
            return Result.fail(ResultCode.NOT_LOGIN);}

        // ====================== 2. 查询数据库，获取最新用户资料 ======================
        SysUser user = sysUserService.getById(loginUser.getUserId());
        if (user == null) {
            return Result.fail(ResultCode.USER_NOT_EXIST);
        }

        // ====================== 3. 转换 VO ======================
        SysUserSimpleVO vo = BeanUtil.copyProperties(user, SysUserSimpleVO.class);

        // 昵称为空时，默认显示用户名
        if (StrUtil.isBlank(vo.getNickName())) {
            vo.setNickName(user.getUserName());
        }

        // ====================== 4. 可选：把角色/权限直接从 LoginUserContext 拿，避免数据库访问 ======================
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());

        // ====================== 5. 返回 ======================
        return Result.success(vo);
    }


    /**
     * 统一记录登录日志
     * @param userId 用户ID
     * @param userName 用户名
     * @param nickName 昵称
     * @param request 请求对象
     * @param loginStatus 登录状态 1成功 0失败
     * @param failReason 失败原因
     */
    private void recordLoginLog(Long userId, String userName, String nickName,
                                HttpServletRequest request,
                                Integer loginStatus, String failReason) {
        try {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            cn.hutool.http.useragent.UserAgent ua = cn.hutool.http.useragent.UserAgentUtil.parse(userAgent);
            String browser = ua.getBrowser().getName();
            String os = ua.getOs().getName();
            String loginAddress = ipLocationService.resolveAddress(ip);
            // 构建登录日志
            SysLoginLog log = new SysLoginLog();
            log.setId(IdUtil.getSnowflakeNextId()); // 雪花ID
            log.setUserId(userId);
            log.setUserName(userName);
            log.setNickName(nickName);
            log.setLoginIp(ip);
            log.setLoginAddress(loginAddress);
            log.setBrowser(browser);
            log.setOperatingSystem(os);
            log.setLoginStatus(loginStatus);
            log.setFailReason(failReason);
            log.setUserAgent(userAgent);
            log.setLoginTime(LocalDateTime.now());
            log.setCreatedTime(LocalDateTime.now());

            // 异步保存日志，不影响登录性能
            loginLogService.save(log);
        } catch (Exception e) {
            // 日志记录异常不影响主流程
            log.error("记录登录日志失败", e);
        }
    }
}