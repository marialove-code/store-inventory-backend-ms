package com.inventory.common.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析工具（公共模块）。
 * <p>
 * 用于登录日志、操作审计等场景：从请求头中按常见代理链路取值，
 * 再回退到 {@link HttpServletRequest#getRemoteAddr()}。
 * </p>
 * <p>
 * 依赖 {@code jakarta.servlet-api}（inventory-common 以 provided 引入），
 * 各 Web 服务运行时由 Spring Boot 提供实现。
 * </p>
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP。
     * <p>
     * 优先级：X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP →
     * HTTP_CLIENT_IP → HTTP_X_FORWARDED_FOR → remoteAddr。
     * 多级代理时取逗号分隔的第一段；本机 IPv6 回环统一映射为 127.0.0.1。
     * </p>
     *
     * @param request 当前 HTTP 请求
     * @return 解析出的 IP 字符串（可能为 null，取决于容器）
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多级代理：X-Forwarded-For 可能是 "client, proxy1, proxy2"
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 本机 IPv6 回环地址统一成 IPv4，便于日志展示与比对
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
