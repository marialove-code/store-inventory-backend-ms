package com.inventory.stock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 库存服务启动类。
 * <p>
 * P3：扫描 {@code com.inventory}，以便加载：
 * <ul>
 *   <li>{@code com.inventory.stock.*} — 配置、异常、命令式 API、探活</li>
 *   <li>{@code com.inventory.modules.invertory.*} — 库存列表/入库/出库/流水/预警</li>
 * </ul>
 * 不引入 Nacos / Security / Feign；连同一 PostgreSQL。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory")
// 只扫 Mapper 接口所在包，避免把 Service 接口误注册成 Mapper Bean
@MapperScan({
        "com.inventory.modules.invertory.stock.mapper",
        "com.inventory.modules.invertory.stockflow.mapper",
        "com.inventory.modules.invertory.stockin.mapper",
        "com.inventory.modules.invertory.stockout.mapper"
})
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
