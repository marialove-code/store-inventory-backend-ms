package com.inventory.common.response;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * 统一返回体。
 *
 * @param <T> 数据类型
 * @author inventory
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务码：0 表示成功
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 数据体
     */
    private T data;

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    /**
     * 失败（默认失败码）
     */
    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.FAIL.getCode(), message);
    }

    /**
     * 失败（指定码与信息）
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /**
     * 失败（枚举定义）
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }
}
