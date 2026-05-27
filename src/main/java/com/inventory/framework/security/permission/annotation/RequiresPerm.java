package com.inventory.framework.security.permission.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 用于接口权限控制
 * 示例：
 * {@code @RequiresPerm("user:add")}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPerm {

    /**
     * 权限标识
     */
    String value();
}