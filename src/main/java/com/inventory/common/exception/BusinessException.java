package com.inventory.common.exception;

import com.inventory.common.result.ResultCode;

import lombok.Getter;

/**
 * 业务异常（仅携带统一错误码与提示信息，不包含任何具体业务域模型）。
 *
 * @author inventory
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
