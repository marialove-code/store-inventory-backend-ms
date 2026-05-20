package com.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 *
 * 这里只做：
 * 1. 跨域配置
 *
 * 登录校验已经全部交给：
 * Spring Security + JWT Filter
 *
 * 所以不再使用 LoginInterceptor
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")

                // 前端地址
                .allowedOrigins("http://localhost:5173")

                // 允许请求方式
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )

                // 允许请求头
                .allowedHeaders("*")

                // 允许携带Cookie
                .allowCredentials(true)

                // 预检请求缓存时间
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /upload/avatar/ 路径到本地文件夹
        // 映射头像目录
        registry.addResourceHandler("/upload/avatar/**")
                .addResourceLocations("file:E:/Image/upload/avatar/");
    }
}