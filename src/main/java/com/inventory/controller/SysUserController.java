package com.inventory.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.annotation.OperationLog;
import com.inventory.annotation.RateLimit;
import com.inventory.annotation.RequiresPerm;
import com.inventory.common.enums.OperationTypeEnum;
import com.inventory.common.result.Result;
import com.inventory.entity.*;
import com.inventory.entity.login.LoginUserVO;
import com.inventory.service.SysPermissionService;
import com.inventory.service.SysRoleService;
import com.inventory.service.SysUserRoleService;
import com.inventory.service.SysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * 模块：系统用户 / 登录认证 / 个人信息 / 用户管理
 * 安全：登录拦截 + 接口限流 + 操作日志
 *
 * 【大厂改造】：使用 Token + Redis 实现无状态登录
 * 1. 关闭浏览器 → Token 自动失效
 * 2. 重启服务 → 用户不掉线
 * 3. 换浏览器必须重新登录
 *
 * @author 资深架构师
 * @date 2026-05-08
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysUserRoleService sysUserRoleService;



    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ============================ 登录认证模块 =============================

    @RateLimit(limit = 5, period = 60, msg = "注册请求过于频繁，请1分钟后再试！")
    @OperationLog(title = "用户注册", type = OperationTypeEnum.REGISTER)
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody SysUserRegisterDTO dto) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", dto.getUserName());
        long count = sysUserService.count(queryWrapper);

        if (count > 0) {
            return Result.fail("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUserName(dto.getUserName());
        user.setPassword(new BCryptPasswordEncoder().encode(dto.getPassword()));
        user.setNickName(dto.getNickName());
        user.setStatus(1);
        user.setIsDeleted(0);

        sysUserService.save(user);

        // ====================== 自动绑定【普通用户】角色 ======================
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());    // 刚注册的用户ID
        userRole.setRoleId(3L);              // 普通用户角色ID固定为3
        sysUserRoleService.save(userRole);
        // ====================================================================

        return Result.success();
    }

    @RateLimit(limit = 3, period = 60, msg = "登录请求过于频繁，请1分钟后再试！")
    @OperationLog(title = "用户登录", type = OperationTypeEnum.LOGIN)
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @Valid @RequestBody SysUserLoginDTO dto) {

        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);
        wrapper.eq(SysUser::getStatus, 1);

        SysUser user = sysUserService.getOne(wrapper);

        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        List<String> roles = sysRoleService.listRoleCodesByUserId(user.getId());
        List<String> permissions = sysPermissionService.listPermCodesByUserId(user.getId());

        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUserName());
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        loginUser.setAdmin(roles.contains("SUPER_ADMIN"));

        redisTemplate.opsForValue().set(
                "token:" + token,
                loginUser,
                1,
                TimeUnit.DAYS
        );

        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userVO);

        return Result.success(result);
    }

    @OperationLog(title = "用户登出", type = OperationTypeEnum.LOGOUT)
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        if (StrUtil.isNotBlank(token)) {
            redisTemplate.delete("token:" + token);
        }
        return Result.success();
    }

    @GetMapping("/current")
    public Result<SysUserSimpleVO> currentUser(@RequestHeader("Authorization") String token) {
        if (StrUtil.isBlank(token)) {
            return Result.fail("未登录");
        }

        // 1. 从 Redis 中获取存入的对象
        Object cachedData = redisTemplate.opsForValue().get("token:" + token);
        if (cachedData == null) {
            return Result.fail("登录已过期，请重新登录");
        }

        // 2. 提取 userId (确保类型安全)
        Long userId;
        if (cachedData instanceof LoginUserVO) {
            userId = ((LoginUserVO) cachedData).getUserId();
        } else {
            // 如果你的 Redis 配置了特定序列化，有时可能会返回 JSON 或 Map，这里做下兼容
            return Result.fail("登录状态异常");
        }

        // 3. 根据 ID 查询数据库
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在或已被删除");
        }

        // 4. 返回 VO 对象
        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        return Result.success(userVO);
    }

    // ============================ 用户管理模块 =============================

    @RateLimit(limit = 30, period = 60)
    @GetMapping("/list")
    @RequiresPerm("system:user:list")
    public Result<Page<SysUserListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getIsDeleted, 0);

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUserName, keyword)
                    .or().like(SysUser::getNickName, keyword)
                    .or().like(SysUser::getPhone, keyword)
            );
        }

        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = sysUserService.page(page, wrapper);

        Page<SysUserListVO> voPage = new Page<>(
                userPage.getCurrent(),
                userPage.getSize(),
                userPage.getTotal()
        );

        voPage.setRecords(
                userPage.getRecords().stream()
                        .map(u -> BeanUtil.copyProperties(u, SysUserListVO.class))
                        .collect(Collectors.toList())
        );

        return Result.success(voPage);
    }

    @GetMapping("/user/{id}")
    @RequiresPerm("system:user:list")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        user.setPassword(null);
        return Result.success(user);
    }

    @RateLimit(limit = 10, period = 60)
    @OperationLog(title = "修改个人信息", type = OperationTypeEnum.UPDATE)
    @PutMapping("/user/{id}")
    public Result<Void> updateUser(
            @PathVariable Long id,
            @RequestBody SysUser user
    ) {
        SysUser exist = sysUserService.getById(id);
        if (exist == null || exist.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getId, id);

        if (StrUtil.isNotBlank(user.getNickName())) wrapper.set(SysUser::getNickName, user.getNickName());
        if (user.getSex() != null) wrapper.set(SysUser::getSex, user.getSex());
        if (user.getAge() != null) wrapper.set(SysUser::getAge, user.getAge());
        if (StrUtil.isNotBlank(user.getPhone())) wrapper.set(SysUser::getPhone, user.getPhone());
        if (StrUtil.isNotBlank(user.getEmail())) wrapper.set(SysUser::getEmail, user.getEmail());
        if (StrUtil.isNotBlank(user.getAvatar())) wrapper.set(SysUser::getAvatar, user.getAvatar());

        if (StrUtil.isNotBlank(user.getPassword())) {
            wrapper.set(SysUser::getPassword, new BCryptPasswordEncoder().encode(user.getPassword()));
        }

        sysUserService.update(wrapper);
        return Result.success();
    }

    // ============================ 管理员操作模块 =============================

    @OperationLog(title = "修改用户状态", type = OperationTypeEnum.UPDATE)
    @PutMapping("/{id}/status")
    @RequiresPerm("system:user:changeStatus")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        if (id == null || (status != 0 && status != 1)) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        user.setStatus(status);
        sysUserService.updateById(user);
        return Result.success();
    }

    @RateLimit(limit = 5, period = 60)
    @OperationLog(title = "重置用户密码", type = OperationTypeEnum.UPDATE)
    @PutMapping("/{id}/resetPassword")
    @RequiresPerm("system:user:resetPwd")
    public Result<Void> resetPassword(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        String defaultPwd = "123456";
        user.setPassword(new BCryptPasswordEncoder().encode(defaultPwd));
        sysUserService.updateById(user);

        return Result.success();
    }

    @OperationLog(title = "删除用户", type = OperationTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    @RequiresPerm("system:user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        user.setIsDeleted(1);
        sysUserService.updateById(user);
        return Result.success();
    }

    @OperationLog(title = "批量删除用户", type = OperationTypeEnum.DELETE)
    @DeleteMapping("/batch")
    @RequiresPerm("system:user:delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的用户");
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.in(SysUser::getId, ids).set(SysUser::getIsDeleted, 1);
        sysUserService.update(updateWrapper);

        return Result.success();
    }
}