package com.inventory.framework.web.ratelimit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.framework.web.ratelimit.annotation.RateLimit;
import com.inventory.common.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(1)
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 环绕增强：所有加了 @RateLimit 的接口都会被拦截
    @Around("@annotation(com.inventory.framework.web.ratelimit.annotation.RateLimit)")
    public Object limit(ProceedingJoinPoint point) throws Throwable {
        // 1. 获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        // 2. 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 3. 生成唯一KEY：接口 + IP
        String ip = getIp(request);
        String uri = request.getRequestURI();
        String key = "limit:" + uri + ":" + ip;

        // 4. 读取注解配置
        int limit = rateLimit.limit();
        int period = rateLimit.period();
        TimeUnit unit = rateLimit.timeUnit();
        String msg = rateLimit.msg();

        // 5. Redis 计数
        Long current = redisTemplate.opsForValue().increment(key, 1);
        if (current == 1) {
            redisTemplate.expire(key, period, unit);
        }

        // 6. 超过限制 → 拦截
        if (current > limit) {
            response.setContentType("application/json;charset=utf-8");
            objectMapper.writeValue(response.getWriter(), Result.fail(msg));
            return null;
        }

        // 7. 正常放行
        return point.proceed();
    }

    // 获取真实IP
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}