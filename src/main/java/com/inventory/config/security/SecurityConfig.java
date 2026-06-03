package com.inventory.config.security;

import com.inventory.framework.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置类
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * JWT过滤器
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 白名单接口
     */
    private static final String[] WHITE_LIST = {

            "/auth/login",
            "/auth/register",
            "/auth/refreshToken",
            // 这里改掉
            "/upload/avatar/**",

            "/doc.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    /**
     * 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    /**
     * SpringSecurity过滤器链
     */
    /**
     * ============================================
     * SpringSecurity过滤器链
     * ============================================
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /**
                 * 开启跨域
                 */
                .cors(cors -> {})

                /**
                 * 关闭CSRF
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /**
                 * JWT无状态登录
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /**
                 * 接口权限控制
                 */
                .authorizeHttpRequests(auth -> auth
                        /**
                         * 白名单放行
                         */
                        .requestMatchers(WHITE_LIST).permitAll()

                        /**
                         * 其他请求必须登录
                         */
                        .anyRequest().authenticated()
                )

                /**
                 * 【优化】配置认证失败处理器
                 * 原因：SpringSecurity默认返回HTML错误页，前端不好处理
                 * 改成返回JSON，code=1102，前端自动刷新token
                 */
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(authenticationEntryPoint())
                )

                /**
                 * 添加JWT过滤器
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * ============================================
     * 认证失败处理器
     * ============================================
     *
     * 作用：当用户未登录或token无效时，返回统一的JSON错误格式
     * 而不是SpringSecurity默认的HTML错误页
     *
     * 返回格式：{"code": 1102, "msg": "Token已过期"}
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");

            /**
             * 1102 是前后端约定的Token过期错误码
             * 前端拦截器检测到 code=1102 会自动调用 /auth/refresh 刷新token
             */
            String json = "{\"code\":1102,\"msg\":\"\"}";
            response.getWriter().write(json);
        };
    }
}