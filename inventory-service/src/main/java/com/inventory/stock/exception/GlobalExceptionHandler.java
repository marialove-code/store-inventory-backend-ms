package com.inventory.stock.exception;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 库存服务全局异常处理（精简版）。
 * <p>
 * 将业务异常 / 非法状态 / 未知异常统一转为 {@link Result}，
 * 避免堆栈直接暴露给调用方（订单服务后续通过 HTTP 调用时也能拿到可读 message）。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：携带自定义业务码。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("【业务异常】code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 非法状态：如 V4 原子锁库存不足时抛出的 {@link IllegalStateException}。
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException ex) {
        log.warn("【非法状态】{}", ex.getMessage());
        return Result.fail(ResultCode.FAIL.getCode(), ex.getMessage());
    }

    /**
     * 兜底：未预期异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("【系统未知异常】", ex);
        return Result.fail(ResultCode.FAIL.getCode(),
                ex.getMessage() != null ? ex.getMessage() : ResultCode.FAIL.getMessage());
    }
}
