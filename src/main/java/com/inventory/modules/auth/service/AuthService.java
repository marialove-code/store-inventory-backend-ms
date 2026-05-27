package com.inventory.modules.auth.service;

import com.inventory.common.response.Result;
import com.inventory.modules.auth.dto.SysUserLoginDTO;
import com.inventory.modules.auth.dto.SysUserRegisterDTO;
import com.inventory.modules.system.user.vo.SysUserSimpleVO;
import com.inventory.modules.auth.vo.LoginTokenVO;
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