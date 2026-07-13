package com.inventory.framework.security.permission.aspect;

import com.inventory.framework.security.permission.annotation.RequiresPerm;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.framework.security.exception.PermissionException;
import com.inventory.modules.system.permission.service.SysPermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
@Order(3)
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

        // 1. 获取接口需要的权限标识
        String needPerm = requiresPerm.value();
        if (StringUtils.isEmpty(needPerm)) {
            return; // 接口不需要权限
        }

        // 2. 获取当前登录用户
        LoginUserVO loginUser = LoginUserContext.getUser();
        if (loginUser == null) {
            throw new PermissionException("用户未登录");
        }
        // 3. 获取当前登录用户ID
        Long userId = loginUser.getUserId();
        if (userId == null) {
            throw new PermissionException("用户未登录");
        }

        // ==============================================
        // ✅ 【优化】超级管理员（SUPER_ADMIN）直接放行所有接口
        // ==============================================
        if (loginUser.getRoles().contains("SUPER_ADMIN")) {
            return;
        }
        // 3. 查询用户权限列表
        List<String> userPerms = sysPermissionService.listPermCodesByUserId(userId);
        // 5. 判断权限
        if (userPerms == null || !userPerms.contains(needPerm)) {
            log.warn("权限校验失败，userId={}, needPerm={}", userId, needPerm);
            throw new PermissionException("无权限访问");
        }
    }
}