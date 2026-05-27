package com.inventory.common.exception;

import com.inventory.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * 特点：
 * 1. 统一错误码
 * 2. 统一异常消息
 * 3. 支持自定义消息覆盖
 * 4. 方便全局异常处理
 *
 * 使用方式：
 *
 * throw new BusinessException(ResultCode.LOGIN_ERROR);
 *
 * 或：
 *
 * throw new BusinessException(
 *         ResultCode.FAIL,
 *         "自定义错误信息"
 * );
 *
 * @author inventory
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 使用 ResultCode
     */
    public BusinessException(ResultCode resultCode) {

        super(resultCode.getMessage());

        this.code = resultCode.getCode();
    }

    /**
     * 使用 ResultCode + 自定义消息
     */
    public BusinessException(
            ResultCode resultCode,
            String overrideMessage) {

        super(overrideMessage);

        this.code = resultCode.getCode();
    }

    /**
     * 自定义 code + message
     */
    public BusinessException(
            int code,
            String message) {

        super(message);

        this.code = code;
    }
}