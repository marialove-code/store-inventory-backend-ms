package com.inventory.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.result.Result;
import com.inventory.context.LoginUserContext;
import com.inventory.entity.login.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RedisTemplate<String, Object> redisTemplate;

    public LoginInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // 白名单放行
        if (uri.contains("/auth/login")
                || uri.contains("/auth/register")
                || uri.contains("/auth/logout")
                || uri.contains("/auth/current")
                || uri.contains("/doc.html")
                || uri.contains("/swagger")
                || uri.contains("/v3/api-docs")
                || uri.contains("/webjars")
                || uri.contains("/favicon.ico")) {
            return true;
        }

        // 获取Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            response.setContentType("application/json;charset=utf-8");
            OBJECT_MAPPER.writeValue(response.getWriter(), Result.fail("未登录，请重新登录"));
            return false;
        }

        // 从Redis获取用户
        Object loginUserObj = redisTemplate.opsForValue().get("token:" + token);
        if (loginUserObj == null) {
            response.setContentType("application/json;charset=utf-8");
            OBJECT_MAPPER.writeValue(response.getWriter(), Result.fail("登录已过期，请重新登录"));
            return false;
        }

        // 转换并存入ThreadLocal（这里是关键修改点）
        LoginUserVO loginUser = (LoginUserVO) loginUserObj;
        LoginUserContext.setUser(loginUser);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 必须清理，防止线程复用导致数据污染
        LoginUserContext.clear();
    }
}