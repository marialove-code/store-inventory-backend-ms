package com.inventory.config;

import com.inventory.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置类
 *
 * 负责：
 * 1. JWT认证
 * 2. 接口权限控制
 * 3. 无状态登录
 * 4. Security过滤器链
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * JWT认证过滤器
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 白名单接口
     * 不需要登录即可访问
     */
    private static final String[] WHITE_LIST = {

            // 登录注册
            "/auth/login",
            "/auth/register",
            // Token刷新
            "/auth/refreshToken",

            // Swagger / Knife4j
            "/doc.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    /**
     * BCrypt密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 核心过滤链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http

                /**
                 * 开启跨域
                 *
                 * 会自动读取 WebConfig 中的跨域配置
                 */
                .cors(cors -> {
                })

                /**
                 * 关闭CSRF
                 *
                 * 前后端分离 + JWT 项目必须关闭
                 */
                .csrf(csrf -> csrf.disable())

                /**
                 * 禁用Session
                 *
                 * JWT是无状态登录
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /**
                 * 配置接口权限
                 */
                .authorizeHttpRequests(auth -> auth

                        /**
                         * 白名单放行
                         */
                        .requestMatchers(WHITE_LIST)
                        .permitAll()

                        /**
                         * 其他所有请求必须登录
                         */
                        .anyRequest()
                        .authenticated()
                )

                /**
                 * 添加JWT过滤器
                 *
                 * 必须放在：
                 * UsernamePasswordAuthenticationFilter 前面
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        // 构建并返回
        return http.build();
    }
}