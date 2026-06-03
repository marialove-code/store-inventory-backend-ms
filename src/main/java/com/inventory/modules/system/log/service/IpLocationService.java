package com.inventory.modules.system.log.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;

@Slf4j
@Service
public class IpLocationService {

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource("ip2region/ip2region_v4.xdb").getInputStream()) {
            byte[] dbBin = in.readAllBytes();
            this.searcher = Searcher.newWithBuffer(dbBin);
        } catch (Exception e) {
            log.error("加载 ip2region 失败", e);
            throw new IllegalStateException("ip2region 初始化失败", e);
        }
    }

    /**
     * 解析 IP 为登录地点
     * ip2region 返回格式：国家|区域|省份|城市|ISP
     * 例：中国|0|广东省|深圳市|电信
     */
    public String resolveAddress(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }

        // 内网 IP
        if (isInternalIp(ip)) {
            return "内网IP";
        }

        try {
            String region = searcher.search(ip.trim());
            return formatRegion(region);
        } catch (Exception e) {
            log.warn("IP 地址解析失败, ip={}", ip, e);
            return "未知";
        }
    }

    private String formatRegion(String region) {
        if (!StringUtils.hasText(region)) {
            return "未知";
        }

        String[] parts = region.split("\\|");
        if (parts.length < 5) {
            return region;
        }

        String country = clean(parts[0]);
        String province = clean(parts[2]);
        String city = clean(parts[3]);
        String isp = clean(parts[4]);

        // 境外
        if (StringUtils.hasText(country) && !"中国".equals(country)) {
            return country;
        }

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(province)) {
            sb.append(province);
        }
        if (StringUtils.hasText(city) && !city.equals(province)) {
            sb.append(city);
        }
        if (sb.length() == 0 && StringUtils.hasText(isp)) {
            sb.append(isp);
        }

        return sb.length() > 0 ? sb.toString() : "未知";
    }

    private String clean(String value) {
        return "0".equals(value) ? "" : value;
    }

    private boolean isInternalIp(String ip) {
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("127.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")
                || "localhost".equalsIgnoreCase(ip);
    }
}