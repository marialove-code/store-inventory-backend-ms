package com.inventory.common.base;

import com.inventory.common.result.Result;

/**
 * 控制器基类：提供统一返回封装方法。
 *
 * @author inventory
 */
public abstract class BaseController {

    /**
     * 成功（无数据体）
     */
    protected <T> Result<T> success() {
        return Result.success();
    }

    /**
     * 成功（带数据体）
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 失败（自定义消息）
     */
    protected <T> Result<T> fail(String message) {
        return Result.fail(message);
    }

    /**
     * 失败（业务码与消息）
     */
    protected <T> Result<T> fail(int code, String message) {
        return Result.fail(code, message);
    }
}
