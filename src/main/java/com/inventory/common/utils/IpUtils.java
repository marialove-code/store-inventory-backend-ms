package com.inventory.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import cn.hutool.core.util.StrUtil;

public class IpUtils {

    public static String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (StrUtil.isBlank(ip)
                || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getHeader("Proxy-Client-IP");
        }

        if (StrUtil.isBlank(ip)
                || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (StrUtil.isBlank(ip)
                || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (StrUtil.isBlank(ip)
                || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (StrUtil.isBlank(ip)
                || "unknown".equalsIgnoreCase(ip)) {

            ip = request.getRemoteAddr();
        }

        // 多级代理
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 本地IPv6
        if ("0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)) {

            ip = "127.0.0.1";
        }

        return ip;
    }
}