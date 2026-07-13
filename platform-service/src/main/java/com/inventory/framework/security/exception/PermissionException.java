package com.inventory.framework.security.exception;

/**
 * 权限校验异常
 */
public class PermissionException extends RuntimeException {

    public PermissionException(String message) {
        super(message);
    }
}