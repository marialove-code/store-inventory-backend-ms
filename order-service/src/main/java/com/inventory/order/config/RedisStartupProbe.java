package com.inventory.order.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动时打印 Redis 实际生效配置来源（不打印密码明文），便于排查 AUTH / 连错主机。
 */
@Slf4j
@Component
public class RedisStartupProbe {

    private final Environment environment;

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.password:}")
    private String password;

    public RedisStartupProbe(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logRedisEffectiveConfig() {
        boolean hasPassword = StringUtils.hasText(password);
        String pwdSource = resolvePropertySource("spring.data.redis.password");
        String hostSource = resolvePropertySource("spring.data.redis.host");

        log.info("======== Redis 生效配置（排查用）========");
        log.info("host={} (来源: {})", host, hostSource);
        log.info("port={}  database={}", port, database);
        log.info("password已设置={}  length={}  (来源: {})",
                hasPassword, hasPassword ? password.length() : 0, pwdSource);
        log.info("环境变量 REDIS_PWD 是否存在={}", environment.containsProperty("REDIS_PWD")
                || System.getenv("REDIS_PWD") != null);
        log.info("若 password已设置=true 但服务器未 requirepass，会出现 AUTH 报错");
        log.info("请同时检查 Nacos：order-service.yaml 是否写了 spring.data.redis.password");
        log.info("========================================");
    }

    private String resolvePropertySource(String key) {
        var pvs = ((org.springframework.core.env.AbstractEnvironment) environment).getPropertySources();
        for (org.springframework.core.env.PropertySource<?> ps : pvs) {
            if (ps.containsProperty(key)) {
                return ps.getName();
            }
        }
        return "未找到/使用默认值";
    }
}
