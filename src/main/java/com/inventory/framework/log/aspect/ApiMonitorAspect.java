package com.inventory.framework.log.aspect;

import com.inventory.modules.monitor.apimonitor.entity.SysApiMonitor;
import com.inventory.modules.monitor.apimonitor.mapper.SysApiMonitorMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
public class ApiMonitorAspect {

    private final SysApiMonitorMapper mapper;

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Pointcut("execution(* com.inventory.modules..controller..*(..))")
    public void apiPointcut() {}

    @Before("apiPointcut()")
    public void before() {
        START_TIME.set(System.currentTimeMillis());
    }

    @AfterReturning("apiPointcut()")
    public void afterReturning(JoinPoint point) {

        save(point, true, null);
    }

    @AfterThrowing(pointcut = "apiPointcut()", throwing = "ex")
    public void afterThrowing(JoinPoint point, Exception ex) {

        save(point, false, ex.getMessage());
    }

    private void save(JoinPoint point, boolean success, String errorMsg) {

        long time = System.currentTimeMillis() - START_TIME.get();

        // 1.强转拿到ServletRequestAttributes，再获取request
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            return;
        }
        HttpServletRequest request = attr.getRequest();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        SysApiMonitor log = new SysApiMonitor();
        log.setApiPath(uri);
        log.setRequestMethod(method);
        log.setResponseTime(time);
        log.setSuccessFlag(success ? 1 : 0);
        log.setCreateTime(LocalDateTime.now());

        mapper.insert(log);

        START_TIME.remove();
    }
}