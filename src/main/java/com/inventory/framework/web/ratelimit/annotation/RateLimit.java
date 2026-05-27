package com.inventory.framework.web.ratelimit.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)       // 只能加在接口方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface RateLimit {

    // 限制访问次数
    int limit() default 10;

    // 限制时间
    int period() default 60;

    // 时间单位
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 超过限制提示语
    String msg() default "请求过于频繁，请稍后再试";
}