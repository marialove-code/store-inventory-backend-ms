package com.inventory.controller;

import com.inventory.annotation.OperationLog;
import com.inventory.annotation.RateLimit;
import com.inventory.common.enums.OperationTypeEnum;
import com.inventory.common.result.Result;
import com.inventory.entity.SysUserLoginDTO;
import com.inventory.entity.SysUserRegisterDTO;
import com.inventory.entity.SysUserSimpleVO;
import com.inventory.entity.login.LoginTokenVO;
import com.inventory.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（工程化拆分）
 * 职责：登录、注册、登出、刷新Token、获取当前用户
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @RateLimit(limit = 5, period = 60, msg = "注册请求频繁")
    @OperationLog(title = "用户注册", type = OperationTypeEnum.REGISTER)
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUserRegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    /**
     * 登录
     */
    @RateLimit(limit = 3, period = 60, msg = "登录请求频繁")
    @OperationLog(title = "用户登录", type = OperationTypeEnum.LOGIN)
    @PostMapping("/login")
    public Result<LoginTokenVO> login(@RequestBody SysUserLoginDTO dto) {
        System.out.println("频繁调用");
        return Result.success(authService.login(dto));
    }

    /**
     * 登出
     */
    @OperationLog(title = "用户登出", type = OperationTypeEnum.LOGOUT)
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader("${jwt.header}") String token,
            @RequestHeader(value = "refreshToken", required = false) String refreshToken) {
        authService.logout(token, refreshToken);
        return Result.success();
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refreshToken")
    public Result<LoginTokenVO> refreshToken(@RequestParam String refreshToken) {
        return Result.success(authService.refreshToken(refreshToken));
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/current")
    public Result<SysUserSimpleVO> current() {
        return Result.success(authService.currentUser());
    }
}