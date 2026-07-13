package com.inventory.modules.auth.service;

public interface UserSessionService {

    /**
     * 删除指定设备登录态
     */
    void logoutByToken(Long userId, String accessToken);

    /**
     * 踢用户下线
     */
    void kickUserOffline(Long userId);

    /**
     * 下线其他设备
     */
    Integer kickOtherDevices();

}