package com.inventory.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI 服务启动类。
 * <p>
 * 扫描 {@code com.inventory}：AI 业务、精简看板聚合、运维日志/库存 Mapper。
 * {@link EnableDiscoveryClient}：向 Nacos 注册本服务实例；会话默认 JVM memory；连同一 PostgreSQL。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory")
@EnableDiscoveryClient
@MapperScan({
        "com.inventory.modules.invertory.stock.mapper",
        "com.inventory.modules.system.log.mapper"
})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
