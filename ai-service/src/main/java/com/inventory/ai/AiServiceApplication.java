package com.inventory.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 服务启动类。
 * <p>
 * 扫描 {@code com.inventory}：AI 业务、精简看板聚合、运维日志/库存 Mapper。
 * 本阶段无 Nacos / Security / Feign；会话默认 JVM memory；连同一 PostgreSQL。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory")
@MapperScan({
        "com.inventory.modules.invertory.stock.mapper",
        "com.inventory.modules.system.log.mapper"
})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
