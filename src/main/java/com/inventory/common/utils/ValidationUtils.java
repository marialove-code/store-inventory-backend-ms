package com.inventory.common.utils;

import java.util.Set;
import java.util.stream.Collectors;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.ResultCode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * 参数校验工具（基于 Jakarta Validation）。
 *
 * @author inventory
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * 快速失败：存在任意校验错误则抛出业务异常
     */
    public static <T> void validate(Validator validator, T bean, Class<?>... groups) {
        if (validator == null || bean == null) {
            return;
        }
        Set<ConstraintViolation<T>> violations = validator.validate(bean, groups);
        if (violations.isEmpty()) {
            return;
        }
        String msg = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        throw new BusinessException(ResultCode.PARAM_ERROR, msg);
    }
}
