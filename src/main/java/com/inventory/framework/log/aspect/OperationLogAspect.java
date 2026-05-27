package com.inventory.framework.log.aspect;

import cn.hutool.json.JSONUtil;
import com.inventory.framework.log.annotation.OperationLog;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.inventory.modules.system.user.entity.SysUser;
import com.inventory.modules.system.log.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(3)
public class OperationLogAspect {

    private final SysOperationLogService operationLogService;

    @Pointcut("@annotation(com.inventory.framework.log.annotation.OperationLog)")
    public void logPointCut() {}

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setTitle(operationLog.title());
        logEntity.setOperationType(operationLog.type().getDesc());
        logEntity.setRequestMethod(request.getMethod());
        logEntity.setRequestUri(request.getRequestURI());
        logEntity.setIpAddress(getClientIp(request));
        logEntity.setOperateStatus(1);
        logEntity.setCreateTime(LocalDateTime.now());
        logEntity.setUpdateTime(LocalDateTime.now());

        String ua = request.getHeader("User-Agent");
        if (ua != null) {
            logEntity.setBrowser(parseBrowser(ua));
            logEntity.setOs(parseOs(ua));
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            SysUser user = (SysUser) session.getAttribute("loginUser");
            if (user != null) {
                logEntity.setUsername(user.getUserName());
            }
        }

        Object[] args = point.getArgs();
        logEntity.setRequestParams(JSONUtil.toJsonStr(args));

        Object result;
        try {
            result = point.proceed();
        } catch (Exception e) {
            logEntity.setOperateStatus(0);
            logEntity.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            saveLogAsync(logEntity);
        }

        return result;
    }

    @Async
    public void saveLogAsync(SysOperationLog operationLog) {
        try {
            operationLogService.save(operationLog);
        } catch (Exception e) {
            log.error("操作日志保存失败", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Edge")) return "Edge";
        if (ua.contains("Safari")) return "Safari";
        return "Unknown";
    }

    private String parseOs(String ua) {
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "MacOS";
        if (ua.contains("Linux")) return "Linux";
        return "Unknown";
    }
}