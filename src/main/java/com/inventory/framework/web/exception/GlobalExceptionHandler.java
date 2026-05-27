package com.inventory.framework.web.exception;

import java.util.stream.Collectors;

import com.inventory.common.exception.BusinessException;
import com.inventory.framework.security.exception.PermissionException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理（统一日志与返回结构）
 * 优化版：更稳定、更规范、更易维护
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（我们自己手动抛出的异常）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("【业务异常】code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * JSON 参数校验异常（@Valid @RequestBody）
     * 最常用！登录、注册都会走这里
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("【参数校验异常】{}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * form 表单参数绑定异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("【参数绑定异常】{}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 单个参数校验异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(cv -> cv.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("【约束校验异常】{}", msg);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 非法参数
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("【非法参数】{}", ex.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), ex.getMessage());
    }

    /**
     * 空指针异常（单独捕获，方便排查）
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException ex) {
        log.error("【空指针异常】", ex);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), "服务器内部异常（空指针）");
    }

    /**
     * 所有其他异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("【系统未知异常】", ex);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
    /**
     * 权限校验异常处理
     */
    @ExceptionHandler(PermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 返回 403 状态码
    public Result<Void> handlePermissionException(PermissionException ex) {
        log.warn("【权限异常】{}", ex);
        // 假设你的 ResultCode 中有无权限的定义，或者直接 fail
        return Result.fail(ResultCode.FORBIDDEN);
    }
}