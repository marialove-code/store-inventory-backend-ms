package com.inventory.interceptor;

import com.inventory.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 直接注入 RedisTemplate（你已经配好，直接用）
    private final RedisTemplate<String, Object> redisTemplate;

    public LoginInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 放行：登录、注册、获取当前用户、接口文档
        if (uri.contains("/sysUser/login")
                || uri.contains("/sysUser/register")
                || uri.contains("/doc.html")
                || uri.contains("/swagger")
                || uri.contains("/v3/api-docs")
                || uri.contains("/webjars")
                || uri.contains("/favicon.ico")) {
            return true;
        }

        // ===================== 【大厂标准：从请求头拿 Token】 =====================
        String token = request.getHeader("Authorization");

        // 没有 Token = 未登录
        if (token == null || token.isBlank()) {
            response.setContentType("application/json;charset=utf-8");
            OBJECT_MAPPER.writeValue(response.getWriter(), Result.fail("未登录，请重新登录"));
            return false;
        }

        // ===================== 【校验 Redis 中是否存在该 Token】 =====================
        Boolean hasToken = redisTemplate.hasKey("token:" + token);
        if (Boolean.FALSE.equals(hasToken)) {
            response.setContentType("application/json;charset=utf-8");
            OBJECT_MAPPER.writeValue(response.getWriter(), Result.fail("登录已过期，请重新登录"));
            return false;
        }

        // 校验通过 → 放行
        return true;
    }
}