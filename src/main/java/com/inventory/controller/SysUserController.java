package com.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.inventory.common.result.Result;
import com.inventory.entity.SysUser;
import com.inventory.service.SysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sysUser")
public class SysUserController {
    @Resource
    private SysUserService sysUserService;

    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        String aa= "1";
        return Result.success(sysUserService.list());
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Valid SysUser user) {
        // 用BCrypt加密密码
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        // 保存用户
        sysUserService.save(user);
        return Result.success();
    }
    // ======================================
    // 1. 新增【注册接口】（和 add 类似，单独写一个更规范）
    // 前端请求地址：POST /sysUser/register
    // ======================================
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid SysUser user) {

        // 1. 查询用户名是否已经存在（传统非lambda写法）
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", user.getUserName());
        long count = sysUserService.count(queryWrapper);

        // 2. 传统 if-else 判断，后期超级好扩展
        if (count > 0) {
            // 用户名已存在
            return Result.fail("用户名已存在");
        } else {
            // 用户名不存在 → 执行注册
            // 密码加密
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            user.setPassword(encoder.encode(user.getPassword()));

            // 保存用户
            sysUserService.save(user);

            return Result.success();
        }
    }

    // ======================================
    // 2. 新增【登录接口】
    // 前端请求地址：POST /sysUser/login
    // 接收字段：userName + password
    // ======================================
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestBody Map<String, String> params,
            HttpSession session
    ) {
        String userName = params.get("userName");
        String password = params.get("password");

        // 1. 基础非空校验
        if (userName == null || password == null
                || userName.isBlank() || password.isBlank()) {
            return Result.fail("用户名或密码不能为空");
        }

        // 2. 传统QueryWrapper查询用户，不用lambda
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        SysUser user = sysUserService.getOne(queryWrapper);

        // 3. 判断用户是否存在
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 4. 校验BCrypt密码
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            return Result.fail("密码错误");
        }

        // 5. 服务端Session保存登录态（后端鉴权用，前端拿不到）
        session.setAttribute("loginUser", user);

        // 6. 组装要返回给前端的纯净用户信息（不返回密码！）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("userName", user.getUserName());
        userInfo.put("nickName", user.getNickName() == null ? "" : user.getNickName());

        // 返回给前端
        return Result.success(userInfo);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        // 清除登录态
        session.removeAttribute("loginUser");
        return Result.success();
    }
}

