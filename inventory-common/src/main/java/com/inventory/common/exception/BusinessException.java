package com.inventory.common.exception;

import com.inventory.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带业务码，供后续全局异常处理器转换为 {@code Result}。
 * <p>P0 暂不接入全局处理；先放入 common，迁移业务时直接可用。</p>
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
