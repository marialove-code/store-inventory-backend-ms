package com.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 *
 * 这里只做：
 * 1. 跨域配置（从配置文件读取允许的源）
 * 2. 静态资源映射（从配置文件读取上传路径）
 *
 * 登录校验已经全部交给：
 * Spring Security + JWT Filter
 *
 * 所以不再使用 LoginInterceptor
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的前端地址，从配置文件读取
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigin;

    /**
     * 头像上传路径，从配置文件读取
     */
    @Value("${app.upload.avatar-path}")
    private String avatarPath;

    /**
     * 商品图片上传路径 → 【新增】
     */
    @Value("${app.upload.product-image-path}")
    private String productImagePath;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 从配置文件读取允许的源
                .allowedOrigins(allowedOrigin)
                // 允许请求方式
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许请求头
                .allowedHeaders("*")
                // 允许携带Cookie
                .allowCredentials(true)
                // 预检请求缓存时间
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 头像上传映射（你原来的，不动）
        registry.addResourceHandler("/upload/avatar/**")
                .addResourceLocations(avatarPath);

        // 2. 商品图片映射 → 【新增，完美兼容】
        registry.addResourceHandler("/uploads/product/**")
                .addResourceLocations("file:" + productImagePath);

        // 3. 品牌图片映射 → 【新增，完美兼容】
        registry.addResourceHandler("/uploads/brand/**")
                .addResourceLocations("file:" + productImagePath);
    }
}