package com.inventory.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 全局配置
 *
 * 功能说明：
 * 1. 全局跨域配置（支持携带Cookie）
 * 2. 静态资源访问映射（头像 / 商品图片 / 品牌LOGO）
 *
 * 权限控制已交给 Spring Security + JWT
 * 本类不处理登录拦截
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的前端域名（配置文件读取）
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigin;

    /**
     * 头像上传物理路径
     */
    @Value("${app.upload.avatar-path}")
    private String avatarPath;

    /**
     * 商品图片上传物理路径
     */
    @Value("${app.upload.product-image-path}")
    private String productImagePath;

    /**
     * 品牌LOGO上传物理路径
     */
    @Value("${app.upload.brand-image-path}")
    private String brandPath;

    /**
     * 全局跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins()
                .allowedOrigins(allowedOrigin.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 静态资源映射（上传的图片可以直接通过URL访问）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 头像映射
        registry.addResourceHandler("/upload/avatar/**")
                .addResourceLocations("file:" + avatarPath);

        // 2. 商品图片映射
        registry.addResourceHandler("/upload/product/**")
                .addResourceLocations("file:" + productImagePath);

        // 3. 品牌LOGO映射（修复你之前的错误）
        registry.addResourceHandler("/upload/brand/**")
                .addResourceLocations("file:" + brandPath);
    }
}