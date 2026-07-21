package com.inventory.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类。
 * <p>
 * 扫描 {@code com.inventory}，以便加载：
 * <ul>
 *   <li>{@code com.inventory.order.*} — 配置、异常、探活、库存 Feign 客户端</li>
 *   <li>{@code com.inventory.modules.order.*} — 订单/发货/退款业务 + concurrency 压测包</li>
 *   <li>{@code com.inventory.common.*} — 公共枚举/单号工具（一般无需 Bean）</li>
 * </ul>
 * {@link EnableDiscoveryClient}：向 Nacos 注册；{@link EnableFeignClients}：按服务名发现并调用库存。
 * </p>
 */
@SpringBootApplication(
        scanBasePackages = "com.inventory",
        // 使用 OrderRedissonConfig（空密码不 AUTH）；只排除 V2 自动配置（V1 类不是 AutoConfiguration，exclude 会报错）
        excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV2"
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.inventory.order.client")
@EnableScheduling
// 只扫 Mapper 包，避免把 Service 接口误注册成 Mapper Bean
@MapperScan({
        "com.inventory.modules.order.orderinfo.mapper",
        "com.inventory.modules.order.orderdelivery.mapper",
        "com.inventory.modules.order.orderrefund.mapper"
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        // Sentinel 心跳读的是系统属性 csp.sentinel.dashboard.server；
        // 须在 Sentinel 首次初始化前设置，否则控制台左侧一直没有应用。
        if (System.getProperty("csp.sentinel.dashboard.server") == null) {
            System.setProperty("csp.sentinel.dashboard.server", "127.0.0.1:8858");
        }
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
