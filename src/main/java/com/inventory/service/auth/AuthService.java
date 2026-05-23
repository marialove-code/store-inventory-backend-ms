package com.inventory.service.auth;

import com.inventory.common.result.Result;
import com.inventory.entity.SysUserLoginDTO;
import com.inventory.entity.SysUserRegisterDTO;
import com.inventory.entity.SysUserSimpleVO;
import com.inventory.entity.login.LoginTokenVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    /**
     * 注册
     */
    void register(SysUserRegisterDTO dto);

    /**
     * 登录
     */
    LoginTokenVO login(SysUserLoginDTO dto, HttpServletRequest request);

    /**
     * 登出
     */
    void logout(String token, String refreshToken);

    /**
     * 刷新Token
     */
    LoginTokenVO refreshToken(String refreshToken);

    /**
     * 获取当前用户
     */
    Result<SysUserSimpleVO> currentUser();
}