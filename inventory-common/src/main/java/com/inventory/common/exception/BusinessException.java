package com.inventory.common.exception;

import com.inventory.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带业务码，由各服务 {@code GlobalExceptionHandler} 转换为 {@code Result}。
 * <p>
 * 与单体对齐，支持两种常用构造：
 * <ul>
 *   <li>{@code new BusinessException(ResultCode.xxx)} — 直接使用枚举码与默认文案</li>
 *   <li>{@code new BusinessException(code, message)} — 自定义码与文案（跨服务调用失败时常用）</li>
 * </ul>
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码（与 {@link ResultCode} 对齐） */
    private final int code;

    /**
     * 使用 {@link ResultCode} 构造（与单体一致，如 {@code new BusinessException(ResultCode.FAIL)}）。
     *
     * @param resultCode 预定义结果码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用 {@link ResultCode} 的错误码，但覆盖提示文案。
     *
     * @param resultCode       预定义结果码（取其 code）
     * @param overrideMessage  覆盖后的提示信息
     */
    public BusinessException(ResultCode resultCode, String overrideMessage) {
        super(overrideMessage);
        this.code = resultCode.getCode();
    }

    /**
     * 自定义错误码与文案（跨服务解析远端 Result 失败时使用）。
     *
     * @param code    业务错误码
     * @param message 提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
