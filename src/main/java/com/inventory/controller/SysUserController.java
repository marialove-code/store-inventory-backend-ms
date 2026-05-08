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
import com.inventory.common.enums.OperationTypeEnum;
import com.inventory.common.result.Result;
import com.inventory.entity.*;
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

    /**
     * 注入 RedisTemplate，用于存储登录 Token
     * 【大厂标准】：登录状态不存 Session，全部存入 Redis
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ============================ 登录认证模块（敏感接口，全部加防刷）=============================

    /**
     * 用户注册
     * 功能：校验用户名唯一性 → 密码加密 → 保存用户
     * 安全：1分钟最多5次请求，防恶意批量注册
     *
     * @param dto 注册参数：用户名、密码、昵称
     * @return 注册结果
     */
    @RateLimit(limit = 5, period = 60, msg = "注册请求过于频繁，请1分钟后再试！")
    @OperationLog(title = "用户注册", type = OperationTypeEnum.REGISTER)
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody SysUserRegisterDTO dto) {
        // 校验用户名是否已存在
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", dto.getUserName());
        long count = sysUserService.count(queryWrapper);

        if (count > 0) {
            return Result.fail("用户名已存在");
        }

        // 密码加密并构建用户对象
        SysUser user = new SysUser();
        user.setUserName(dto.getUserName());
        user.setPassword(new BCryptPasswordEncoder().encode(dto.getPassword()));
        user.setNickName(dto.getNickName());
        user.setStatus(1);
        user.setIsDeleted(0);

        sysUserService.save(user);
        return Result.success();
    }

    /**
     * 用户登录【大厂 Token + Redis 标准版】
     * 功能：账号密码校验 → 生成 Token → 存入 Redis → 返回前端
     * 安全：1分钟最多3次，防暴力破解密码
     * 特性：关闭浏览器 → Token 失效；重启服务 → 不掉线
     *
     * @param dto 登录参数：用户名、密码
     * @return Token + 脱敏用户信息
     */
    @RateLimit(limit = 3, period = 60, msg = "登录请求过于频繁，请1分钟后再试！")
    @OperationLog(title = "用户登录", type = OperationTypeEnum.LOGIN)
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody SysUserLoginDTO dto) {
        // 查询有效用户（未删除 + 已启用）
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getUserName, dto.getUserName());
        wrapper.eq(SysUser::getIsDeleted, 0);
        wrapper.eq(SysUser::getStatus, 1);

        SysUser user = sysUserService.getOne(wrapper);
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 密码校验
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        // ===================== 【大厂核心】生成唯一 Token =====================
        String token = UUID.randomUUID().toString().replace("-", "");

        // ===================== Token 存入 Redis，有效期 1 天 =====================
        redisTemplate.opsForValue().set("token:" + token, user.getId(), 1, TimeUnit.DAYS);

        // 封装脱敏 VO
        SysUserSimpleVO userVO = BeanUtil.copyProperties(user, SysUserSimpleVO.class);
        if (StrUtil.isBlank(userVO.getNickName())) {
            userVO.setNickName(user.getUserName());
        }

        // 返回 token + 用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userVO);

        return Result.success(result);
    }

    /**
     * 用户登出【大厂标准】
     * 功能：删除 Redis 中的 Token → 强制下线
     * 前端必须在请求头传入：Authorization: token
     */
    @OperationLog(title = "用户登出", type = OperationTypeEnum.LOGOUT)
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        // 从 Redis 中删除 Token，立即失效
        if (StrUtil.isNotBlank(token)) {
            redisTemplate.delete("token:" + token);
        }
        return Result.success();
    }

    /**
     * 获取当前登录用户信息【从 Redis 获取】
     * 功能：前端刷新/初始化时获取登录态
     * 从请求头取 Token → 查 Redis → 查用户 → 返回
     */
    @GetMapping("/current")
    public Result<SysUserSimpleVO> currentUser(@RequestHeader("Authorization") String token) {
        // 1. 校验 Token 是否为空
        if (StrUtil.isBlank(token)) {
            return Result.fail("未登录");
        }

        // 2. 从 Redis 获取用户 ID
        Long userId = (Long) redisTemplate.opsForValue().get("token:" + token);
        if (userId == null) {
            return Result.fail("登录已过期，请重新登录");
        }

        // 3. 查询真实用户
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 4. 返回脱敏信息
        return Result.success(BeanUtil.copyProperties(user, SysUserSimpleVO.class));
    }

    // ============================ 用户管理模块 =============================

    /**
     * 用户分页列表
     * 功能：支持用户名/昵称/手机号搜索 + 状态筛选 + 分页
     * 安全：1分钟30次，防高频刷库
     */
    @RateLimit(limit = 30, period = 60)
    @GetMapping("/list")
    public Result<Page<SysUserListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysUser::getIsDeleted, 0);

        // 多字段模糊搜索
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

        // 转换为VO脱敏返回
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

    /**
     * 根据ID查询用户详情
     * 用途：个人信息页 / 管理员编辑用户页
     * 安全：隐藏密码字段返回
     */
    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 密码脱敏
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新个人信息
     * 功能：支持昵称、性别、手机号、邮箱、头像、密码
     * 安全：1分钟最多10次，防恶意篡改
     */
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

        // 动态更新非空字段
        LambdaUpdateWrapper<SysUser> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysUser::getId, id);

        if (StrUtil.isNotBlank(user.getNickName())) wrapper.set(SysUser::getNickName, user.getNickName());
        if (user.getSex() != null) wrapper.set(SysUser::getSex, user.getSex());
        if (user.getAge() != null) wrapper.set(SysUser::getAge, user.getAge());
        if (StrUtil.isNotBlank(user.getPhone())) wrapper.set(SysUser::getPhone, user.getPhone());
        if (StrUtil.isNotBlank(user.getEmail())) wrapper.set(SysUser::getEmail, user.getEmail());
        if (StrUtil.isNotBlank(user.getAvatar())) wrapper.set(SysUser::getAvatar, user.getAvatar());

        // 密码单独加密
        if (StrUtil.isNotBlank(user.getPassword())) {
            wrapper.set(SysUser::getPassword, new BCryptPasswordEncoder().encode(user.getPassword()));
        }

        sysUserService.update(wrapper);
        return Result.success();
    }

    // ============================ 管理员操作模块 =============================

    /**
     * 修改用户状态（启用/禁用）
     * 用途：管理员封禁违规用户
     */
    @OperationLog(title = "修改用户状态", type = OperationTypeEnum.UPDATE)
    @PutMapping("/{id}/status")
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

    /**
     * 重置用户密码
     * 用途：用户忘记密码 → 管理员重置为默认密码 123456
     * 安全：1分钟最多5次，防恶意滥用
     */
    @RateLimit(limit = 5, period = 60)
    @OperationLog(title = "重置用户密码", type = OperationTypeEnum.UPDATE)
    @PutMapping("/{id}/resetPassword")
    public Result<Void> resetPassword(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("参数错误");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null || user.getIsDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 重置为默认密码并加密
        String defaultPwd = "123456";
        user.setPassword(new BCryptPasswordEncoder().encode(defaultPwd));
        sysUserService.updateById(user);

        return Result.success();
    }

    /**
     * 单个删除用户（逻辑删除）
     */
    @OperationLog(title = "删除用户", type = OperationTypeEnum.DELETE)
    @DeleteMapping("/{id}")
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

    /**
     * 批量删除用户（逻辑删除）
     */
    @OperationLog(title = "批量删除用户", type = OperationTypeEnum.DELETE)
    @DeleteMapping("/batch")
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