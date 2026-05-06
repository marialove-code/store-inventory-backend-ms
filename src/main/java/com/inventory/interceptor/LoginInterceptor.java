package com.inventory.interceptor;

import com.inventory.entity.SysUser;
import com.inventory.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession();
        SysUser loginUser = (SysUser) session.getAttribute("loginUser");

        System.out.println("拦截到的路径：" + request.getServletPath());
        // 没登录
        if (loginUser == null) {
            response.setContentType("application/json;charset=utf-8");
            Result<Object> result = Result.fail("请先登录");
            new ObjectMapper().writeValue(response.getWriter(), result);
            return false;
        }

        // 已登录，放行
        return true;
    }
}