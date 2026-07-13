package com.inventory.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务启动类。
 * <p>
 * P2：扫描 {@code com.inventory}，以便加载：
 * <ul>
 *   <li>{@code com.inventory.order.*} — 配置、异常、RestTemplate、探活、库存客户端</li>
 *   <li>{@code com.inventory.modules.order.*} — 订单/发货/退款业务 + concurrency 压测包</li>
 *   <li>{@code com.inventory.common.*} — 公共枚举/单号工具（一般无需 Bean）</li>
 * </ul>
 * 不依赖库存模块类；库存写操作一律通过 HTTP 调 inventory-service。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory")
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
