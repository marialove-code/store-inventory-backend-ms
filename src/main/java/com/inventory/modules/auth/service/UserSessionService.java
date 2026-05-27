package com.inventory.modules.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserSessionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
    public void kickUserOffline(Long userId) {
        if (userId == null) return;

        // ----------------- 1. 删除该用户所有 AccessToken -----------------
        Set<String> accessKeys = redisTemplate.keys("user:token:" + userId + ":access:*");
        if (accessKeys != null && !accessKeys.isEmpty()) {
            redisTemplate.delete(accessKeys);
        }

        // ----------------- 2. 删除该用户所有 RefreshToken -----------------
        Set<String> refreshKeys = redisTemplate.keys("user:token:" + userId + ":refresh:*");
        if (refreshKeys != null && !refreshKeys.isEmpty()) {
            redisTemplate.delete(refreshKeys);
        }

        // ----------------- 3. 删除该用户权限缓存 -----------------
        String permKey = "user:perm:" + userId;
        redisTemplate.delete(permKey);
    }
}