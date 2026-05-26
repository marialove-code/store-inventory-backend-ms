package com.inventory.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置
 *
 * 核心作用：
 * 1. 将所有 Long 类型统一转为 String 返回给前端
 * 2. 解决 JavaScript Number 精度丢失问题（雪花ID超出 2^53 会失真）
 * 3. 避免每个 VO/DTO 单独加注解，全局统一处理
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder
                // 包装类型 Long
                .serializerByType(Long.class, ToStringSerializer.instance)
                // 基本类型 long
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}