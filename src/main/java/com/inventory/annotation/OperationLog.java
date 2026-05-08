package com.inventory.annotation;

import com.inventory.common.enums.OperationTypeEnum;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    String title() default "";
    OperationTypeEnum type();
}