package com.inventory.filter;

import cn.hutool.core.util.StrUtil;
import com.inventory.common.utils.JwtUtil;
import com.inventory.constant.RedisConstants;
import com.inventory.context.LoginUserContext;
import com.inventory.entity.login.LoginUserVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器
 * 所有请求都会经过这里，负责解析Token、完成SpringSecurity认证
 * 增加 Redis 登录态校验：只有JWT合法 + Redis存在 才算登录成功
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT工具类
    private final JwtUtil jwtUtil;

    // Redis模板：校验登录态是否存在
    private final RedisTemplate<String, Object> redisTemplate;

    // 请求头key：从配置文件读取
    @Value("${jwt.header}")
    private String header;

    // Token前缀：Bearer （带空格）
    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    /**
     * 过滤器核心方法：每次请求都会执行
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 从请求头获取Token
            String token = request.getHeader(header);

            // 2. 如果没有Token 或者 不是以Bearer开头，直接放行（交给后面Security处理）
            if (StrUtil.isBlank(token) || !token.startsWith(tokenPrefix)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 去掉前缀 Bearer ，拿到纯Token
            token = token.substring(tokenPrefix.length()).trim();

            // 4. 校验是否是合法的 AccessToken
            if (!jwtUtil.validateAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // ====================== 【关键新增】Redis 登录态校验 ======================
            // 如果Redis中不存在这个token，说明已退出/被踢/被禁用，直接拒绝
            String redisKey = RedisConstants.LOGIN_TOKEN_KEY + token;
            if (!redisTemplate.hasKey(redisKey)) {
                log.warn("Redis中不存在该登录态，token已失效: {}", token);
                filterChain.doFilter(request, response);
                return;
            }

            // 5. 防止重复认证（上下文中没有认证信息才处理）
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. 从Token解析用户信息
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);

                // ====================== ✅ 只加这 3 行，完全兼容你的代码 ======================
                LoginUserVO loginUserVO = new LoginUserVO();
                loginUserVO.setUserId(userId);
                LoginUserContext.setUser(loginUserVO);
                // ============================================================================

                // TODO 后续可以从Redis中读取真实权限
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();

                // 7. 封装SpringSecurity需要的User对象
                User user = new User(
                        username,
                        "", // 密码不需要，因为JWT已认证
                        authorities
                );

                // 8. 生成SpringSecurity认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );

                // 9. 设置请求详情（IP、Session等）
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 10. 把认证信息存入Security上下文（代表当前用户已登录）
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            // 捕获异常，不中断请求，只打印错误日志
            log.error("JWT认证失败: {}", e.getMessage());
        }

        // 11. 放行，继续执行后面的过滤器
        filterChain.doFilter(request, response);
    }
}