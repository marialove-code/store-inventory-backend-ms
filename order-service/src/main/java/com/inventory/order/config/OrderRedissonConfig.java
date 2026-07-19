package com.inventory.order.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * 自定义 Redisson：密码为空时<strong>不调用 AUTH</strong>。
 * <p>
 * 官方自动配置在 {@code spring.data.redis.password} 为空串时仍可能发 AUTH，
 * 而服务器未配置 {@code requirepass} 时会报：
 * {@code ERR AUTH called without any password configured}。
 * </p>
 */
@Configuration
public class OrderRedissonConfig {

    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:127.0.0.1}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.password:}") String password) {

        Config config = new Config();
        SingleServerConfig single = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectTimeout(10000)
                .setTimeout(3000);

        // 仅非空才设密码，避免对无密码 Redis 误发 AUTH
        if (StringUtils.hasText(password)) {
            single.setPassword(password);
        }

        return Redisson.create(config);
    }

    @Bean
    @Primary
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }
}
