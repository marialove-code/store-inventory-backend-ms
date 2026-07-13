package com.inventory.common.exception;

import com.inventory.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带业务码，由各服务 {@code GlobalExceptionHandler} 转换为 {@code Result}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String overrideMessage) {
        super(overrideMessage);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
