package com.inventory.aop;

import com.inventory.annotation.RequiresPerm;
import com.inventory.context.LoginUserContext;
import com.inventory.exception.PermissionException;
import com.inventory.service.SysPermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class PermissionAspect {

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 匹配权限注解
     */
    @Pointcut("@annotation(requiresPerm)")
    public void permissionPointCut(RequiresPerm requiresPerm) {
    }

    /**
     * 权限校验
     */
    @Before("permissionPointCut(requiresPerm)")
    public void checkPermission(RequiresPerm requiresPerm) {

        // 1. 获取接口需要权限
        String needPerm = requiresPerm.value();

        // 2. 获取当前登录用户ID
        Long userId = LoginUserContext.getUserId();

        if (userId == null) {
            throw new PermissionException("用户未登录");
        }

        // 3. 查询用户权限列表
        List<String> userPerms =
                sysPermissionService.listPermCodesByUserId(userId);

        // 4. 超级管理员直接放行
        if (userPerms.contains("*:*:*")) {
            return;
        }

        // 5. 判断权限
        if (userPerms == null || !userPerms.contains(needPerm)) {

            log.warn("权限校验失败，userId={}, needPerm={}",
                    userId, needPerm);

            throw new PermissionException("无权限访问");
        }
    }
}