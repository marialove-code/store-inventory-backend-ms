package com.inventory.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
@SpringBootApplication(scanBasePackages = "com.inventory")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.inventory.order.client")
// 只扫 Mapper 包，避免把 Service 接口误注册成 Mapper Bean
@MapperScan({
        "com.inventory.modules.order.orderinfo.mapper",
        "com.inventory.modules.order.orderdelivery.mapper",
        "com.inventory.modules.order.orderrefund.mapper"
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
