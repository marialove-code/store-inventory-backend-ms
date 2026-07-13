package com.inventory.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 库存服务启动类。
 * <p>
 * P0：仅验证进程可独立启动；不连库、不鉴权、不注册 Nacos。
 * 后续迁入 StockService、流水、入库出库等。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory.stock")
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
