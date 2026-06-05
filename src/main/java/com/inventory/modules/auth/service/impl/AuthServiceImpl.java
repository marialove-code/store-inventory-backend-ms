package com.inventory.modules.auth.service.impl;
import java.util.Map;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import com.inventory.common.utils.IpUtils;
import com.inventory.framework.security.jwt.JwtUtil;
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
import static com.inventory.common.constants.RedisConstants.*;

/**
 * 认证服务实现类
 * 负责：用户登录、注册、登出、Token刷新、获取当前用户
 * 完全基于 JWT + Redis + SpringSecurity 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 用户服务
    private final SysUserService sysUserService;
    // 角色服务
    private final SysRoleService sysRoleService;
    // 权限服务
    private final SysPermissionService sysPermissionService;
    // 用户角色关联服务
    private final SysUserRoleService sysUserRoleService;
    // JWT工具类
    private final JwtUtil jwtUtil;
    // Redis模板
    private final RedisTemplate<String, Object> redisTemplate;
    // 密码加密器
    private final PasswordEncoder passwordEncoder;
    // 登录日志服务
    private final SysLoginLogService loginLogService;
    // IP地址解析服务
    private final IpLocationService ipLocationService;

    /** AccessToken 过期时间（分钟） */
    @Value("${jwt.access-expire}")
    private Long accessExpire;

    /** RefreshToken 过期时间（分钟） */
    @Value("${jwt.refresh-expire}")
    private Long refreshExpire;

    // ========================== 用户注册 ==========================
    @Override
    public void register(SysUserRegisterDTO dto) {
        // 1. 构造查询条件：根据用户名查询未删除的用户
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);
        // 2. 检查用户名是否已存在
        long count = sysUserService.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }
        // 3. 构建用户实体并加密密码
        SysUser user = new SysUser();
        user.setUserName(dto.getUserName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickName(dto.getNickName());
        user.setStatus(1);
        user.setIsDeleted(0);
        // 4. 保存用户信息
        sysUserService.save(user);
        // 5. 自动绑定普通用户角色（roleId=3）
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        sysUserRoleService.save(userRole);
    }

    // ========================== 用户登录 ==========================
    @Override
    public LoginTokenVO login(SysUserLoginDTO dto, HttpServletRequest request) {
        // 1. 构造查询条件：根据用户名查询未删除的用户
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);
        SysUser user = sysUserService.getOne(wrapper);
        // 2. 用户不存在，记录失败日志并抛出异常
        if (user == null) {
            recordLoginLog(null, dto.getUserName(), null, request, 0, "用户不存在");
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }
        // 3. 用户被禁用，记录失败日志并抛出异常
        if (user.getStatus() == 0) {
            recordLoginLog(user.getId(), user.getUserName(), user.getNickName(), request, 0, "账号已被禁用");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        // 4. 密码校验失败，记录失败日志并抛出异常
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginLog(user.getId(), user.getUserName(), user.getNickName(), request, 0, "密码错误");
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }
        // 5. 查询用户角色集合，判断是否为超级管理员
        List<String> roles = sysRoleService.listRoleCodesByUserId(user.getId());
        boolean isSuperAdmin = roles.contains("SUPER_ADMIN");
        // 6. 查询用户权限集合：超级管理员拥有全部权限，普通用户查询自身权限
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
        // 7. 生成JWT双令牌：访问令牌AccessToken、刷新令牌RefreshToken
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUserName());
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getUserName());
        // 8. 封装登录用户VO对象
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickName(user.getNickName());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        loginUser.setAdmin(isSuperAdmin);
        // 9. 解析请求信息：IP、浏览器、操作系统
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        cn.hutool.http.useragent.UserAgent ua = cn.hutool.http.useragent.UserAgentUtil.parse(userAgent);
        String browser = ua.getBrowser().getName();
        String os = ua.getOs().getName();
        // 10. 设置登录时间与令牌过期时间
        LocalDateTime loginTime = LocalDateTime.now();
        LocalDateTime expireTime = loginTime.plusMinutes(accessExpire);
        loginUser.setIpaddr(ip);
        loginUser.setBrowser(browser);
        loginUser.setOs(os);
        loginUser.setLoginTime(loginTime);
        loginUser.setExpireTime(expireTime);
        // 11. 记录登录成功日志
        recordLoginLog(user.getId(), user.getUserName(), user.getNickName(), request, 1, "登录成功");
        // 12. Redis缓存AccessToken登录态，设置过期时间
        String redisAccessKey = LOGIN_TOKEN_PREFIX + user.getId() + ":access:" + accessToken;
        redisTemplate.opsForValue().set(redisAccessKey, loginUser, accessExpire, TimeUnit.MINUTES);
        // 13. Redis缓存RefreshToken，设置过期时间
        String redisRefreshKey = LOGIN_TOKEN_PREFIX + user.getId() + ":refresh:" + refreshToken;
        redisTemplate.opsForValue().set(redisRefreshKey, user.getId(), refreshExpire, TimeUnit.MINUTES);
        // 14. Redis缓存用户权限信息
        String redisPermKey = USER_PERMISSION_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisPermKey, permissions, accessExpire, TimeUnit.MINUTES);
        // 15. Redis维护用户设备令牌映射关系
        String deviceKey = USER_DEVICE_PREFIX + user.getId();
        redisTemplate.opsForHash().put(deviceKey, accessToken, refreshToken);
        redisTemplate.expire(deviceKey, refreshExpire, TimeUnit.MINUTES);
        // 16. 封装返回前端的令牌VO对象
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



    // ========================== 用户登出 ==========================
    @Override
    public void logout(String token, String refreshToken) {
        // 1. 处理AccessToken：去除Bearer前缀并删除Redis缓存
        if (StrUtil.isNotBlank(token)) {
            token = token.trim();
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            redisTemplate.delete(LOGIN_TOKEN_PREFIX + token);
        }
        // 2. 处理RefreshToken：删除Redis缓存
        if (StrUtil.isNotBlank(refreshToken)) {
            refreshToken = refreshToken.trim();
            redisTemplate.delete(LOGIN_TOKEN_PREFIX + refreshToken);
        }
        // 3. 清空SpringSecurity认证上下文
        SecurityContextHolder.clearContext();
    }

    // ========================== 刷新令牌 ==========================
    @Override
    public LoginTokenVO refreshToken(String refreshToken) {
        // 1. 校验RefreshToken是否为空
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }
        // 2. 校验RefreshToken签名是否合法
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }
        // 3. 从RefreshToken解析用户ID与用户名
        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        // 4. 校验Redis中RefreshToken是否存在（防止被踢下线）
        String refreshKey = LOGIN_TOKEN_PREFIX + userId + ":refresh:" + refreshToken;
        Boolean hasKey = redisTemplate.hasKey(refreshKey);
        if (Boolean.FALSE.equals(hasKey)) {
            throw new BusinessException(ResultCode.LOGIN_STATUS_INVALID);
        }
        // 5. 生成新的AccessToken，复用旧RefreshToken
        String newAccessToken = jwtUtil.createAccessToken(userId, username);
        String newRefreshToken = jwtUtil.createRefreshToken(userId, username);

        // 6. 删除旧RefreshToken，缓存新RefreshToken
        String oldRefreshKey = LOGIN_TOKEN_PREFIX + userId + ":refresh:" + refreshToken;
        redisTemplate.delete(oldRefreshKey);
        String newRefreshKey = LOGIN_TOKEN_PREFIX + userId + ":refresh:" + newRefreshToken;
        redisTemplate.opsForValue().set(newRefreshKey, userId, refreshExpire, TimeUnit.MINUTES);

        // 7. 校验用户状态：是否存在、是否禁用、是否删除
        SysUser user = sysUserService.getUserById(String.valueOf(userId));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        if (user.getIsDeleted() != 0) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 8. 重新查询用户角色与权限
        List<String> roles = sysRoleService.listRoleCodesByUserId(userId);
        List<String> permissions = sysPermissionService.listPermCodesByUserId(userId);
        // 9. 封装新的登录用户信息
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        loginUser.setAdmin(roles.contains("SUPER_ADMIN"));
        // 10. 缓存新的AccessToken
        String accessKey = LOGIN_TOKEN_PREFIX + userId + ":access:" + newAccessToken;
        redisTemplate.opsForValue().set(accessKey, loginUser, accessExpire, TimeUnit.MINUTES);
        // 11. 更新設備令牌映射（RefreshToken轮换）
        String deviceKey = USER_DEVICE_PREFIX + userId;

        // 查找旧映射并删除
        Map<Object, Object> deviceMap = redisTemplate.opsForHash().entries(deviceKey);
        Object oldAccessToken = null;
        for (Map.Entry<Object, Object> entry : deviceMap.entrySet()) {
            Object value = entry.getValue();
            if (refreshToken.equals(value)) {
                oldAccessToken = entry.getKey();
                break;
            }
        }
         // 删除旧映射
        if (oldAccessToken != null) {
            redisTemplate.opsForHash().delete(deviceKey, oldAccessToken);
        }
         // 保存新映射
        redisTemplate.opsForHash().put(deviceKey, newAccessToken, newRefreshToken);

         // 保证设备映射不过期
        redisTemplate.expire(deviceKey, refreshExpire, TimeUnit.MINUTES);

        // 12. 封装返回前端
        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }
        LoginTokenVO vo = new LoginTokenVO();
        vo.setToken(newAccessToken);
        vo.setAccessToken(newAccessToken);

     // 返回新的RefreshToken
        vo.setRefreshToken(newRefreshToken);
        vo.setUser(userVO);
        return vo;
    }

    // ========================== 获取当前登录用户 ==========================
    @Override
    public Result<SysUserSimpleVO> currentUser() {
        // 1. 从上下文获取登录用户信息，JWT过滤器已完成鉴权
        LoginUserVO loginUser = LoginUserContext.getUser();
        if (loginUser == null) {
            return Result.fail(ResultCode.NOT_LOGIN);
        }
        // 2. 查询数据库获取最新用户信息
        SysUser user = sysUserService.getById(loginUser.getUserId());
        if (user == null) {
            return Result.fail(ResultCode.USER_NOT_EXIST);
        }
        // 3. 转换为前端展示VO
        SysUserSimpleVO vo = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(vo.getNickName())) {
            vo.setNickName(user.getUserName());
        }
        // 4. 从上下文直接赋值角色与权限，避免重复查询数据库
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        return Result.success(vo);
    }

    /**
     * 统一记录登录日志（公共方法）
     * @param userId 用户ID
     * @param userName 用户名
     * @param nickName 昵称
     * @param request 请求对象
     * @param loginStatus 登录状态 1成功 0失败
     * @param failReason 失败原因
     */
    private void recordLoginLog(Long userId, String userName, String nickName, HttpServletRequest request, Integer loginStatus, String failReason) {
        try {
            // 1. 解析IP、浏览器、操作系统、登录地址
            String ip = IpUtils.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            cn.hutool.http.useragent.UserAgent ua = cn.hutool.http.useragent.UserAgentUtil.parse(userAgent);
            String browser = ua.getBrowser().getName();
            String os = ua.getOs().getName();
            String loginAddress = ipLocationService.resolveAddress(ip);
            // 2. 构建登录日志实体
            SysLoginLog log = new SysLoginLog();
            log.setId(IdUtil.getSnowflakeNextId());
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
            // 3. 异步保存日志，不影响登录主流程
            loginLogService.save(log);
        } catch (Exception e) {
            // 日志记录异常不抛出，不影响主业务
            log.error("记录登录日志失败", e);
        }
    }
}