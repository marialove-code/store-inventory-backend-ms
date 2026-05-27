package com.inventory.framework.security.context;


import com.inventory.modules.auth.vo.LoginUserVO;

/**
 * 登录用户上下文
 */
public class LoginUserContext {

    /**
     * 当前登录用户
     */
    private static final ThreadLocal<LoginUserVO> LOGIN_USER =
            new ThreadLocal<>();

    /**
     * 设置当前登录用户
     */
    public static void setUser(LoginUserVO loginUser) {
        LOGIN_USER.set(loginUser);
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUserVO getUser() {
        return LOGIN_USER.get();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {

        LoginUserVO loginUser = LOGIN_USER.get();

        if (loginUser == null) {
            return null;
        }

        return loginUser.getUserId();
    }

    /**
     * 清除当前线程数据
     */
    public static void clear() {
        LOGIN_USER.remove();
    }
}