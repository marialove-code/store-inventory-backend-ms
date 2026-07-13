package com.inventory.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务启动类。
 * <p>
 * P0：仅验证进程可独立启动；后续迁入订单/发货/退款，并通过 Feign 调库存服务。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory.order")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
