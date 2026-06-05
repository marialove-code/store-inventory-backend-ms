package com.inventory.modules.auth.service.impl;

import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.inventory.common.constants.RedisConstants.*;

@Service
public class UserSessionServiceImpl implements UserSessionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 删除指定设备登录态
     */
    @Override
    public void logoutByToken(Long userId, String accessToken) {

        String deviceKey =
                USER_DEVICE_PREFIX + userId;

        // 获取对应refreshToken
        Object refreshObj =
                redisTemplate.opsForHash()
                        .get(deviceKey, accessToken);

        String refreshToken =
                refreshObj == null
                        ? null
                        : refreshObj.toString();

        // 删除AccessToken
        String accessKey =
                LOGIN_TOKEN_PREFIX
                        + userId
                        + ":access:"
                        + accessToken;

        redisTemplate.delete(accessKey);

        // 删除RefreshToken
        if (refreshToken != null) {

            String refreshKey =
                    LOGIN_TOKEN_PREFIX
                            + userId
                            + ":refresh:"
                            + refreshToken;

            redisTemplate.delete(refreshKey);
        }

        // 删除设备映射
        redisTemplate.opsForHash()
                .delete(deviceKey, accessToken);
    }

    /**
     * 踢用户下线：删除 Redis 中该用户所有 AccessToken、RefreshToken 和权限缓存
     *
     * 适用场景：
     * 1. 用户被禁用
     * 2. 用户被删除
     * 3. 用户角色/权限变更
     *
     * @param userId 用户ID
     */
    @Override
    public void kickUserOffline(Long userId) {
        if (userId == null) {
            return;
        }

        String deviceKey =
                USER_DEVICE_PREFIX + userId;

        Set<Object> accessTokens =
                redisTemplate.opsForHash()
                        .keys(deviceKey);

        if (accessTokens != null) {

            for (Object tokenObj : accessTokens) {

                logoutByToken(
                        userId,
                        tokenObj.toString()
                );
            }
        }

        // 删除设备索引
        redisTemplate.delete(deviceKey);

        // 删除权限缓存
        redisTemplate.delete(
                USER_PERMISSION_PREFIX + userId
        );
    }

    @Override
    public Integer kickOtherDevices() {

        Long userId =
                LoginUserContext.getUserId();

        String currentAccessToken =
                LoginUserContext.getAccessToken();

        String deviceKey =
                USER_DEVICE_PREFIX + userId;

        Set<Object> accessTokens =
                redisTemplate.opsForHash()
                        .keys(deviceKey);

        if (accessTokens == null ||
                accessTokens.isEmpty()) {
            return 0;
        }

        for (Object tokenObj : accessTokens) {

            String accessToken =
                    tokenObj.toString();

            // 保留当前设备
            if (currentAccessToken.equals(accessToken)) {
                continue;
            }

            logoutByToken(
                    userId,
                    accessToken
            );
        }

        return 1;
    }


}