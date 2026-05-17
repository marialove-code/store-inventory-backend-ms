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

            /**
             * 登录注册
            */
            "/auth/login",
            "/auth/register",
            "/auth/refreshToken",

            /**
             * Swagger
            */
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
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http

                /**
                 * 开启跨域
                 */
                .cors(cors -> {
                })

                /**
                 * 关闭CSRF
                 */
                .csrf(csrf -> csrf.disable())

                /**
                 * JWT无状态登录
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /**
                 * 接口权限控制
                 */
                .authorizeHttpRequests(auth -> auth

                        /**
                         * 白名单放行
                         */
                        .requestMatchers(WHITE_LIST)
                        .permitAll()

                        /**
                         * 其他请求必须登录
                         */
                        .anyRequest()
                        .authenticated()
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
}