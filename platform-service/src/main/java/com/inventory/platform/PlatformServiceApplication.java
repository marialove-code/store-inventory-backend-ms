package com.inventory.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 平台服务启动类（P4）。
 * <p>
 * <b>职责：</b>承接原单体中的认证、商品/分类/品牌、系统用户角色权限、
 * 监控、看板、门店等「平台侧」能力；库存写操作通过 HTTP 调用 inventory-service，
 * 订单域由 order-service 独立承载。
 * </p>
 * <p>
 * 扫描 {@code com.inventory}，以便加载 framework/config/modules 与 {@code com.inventory.platform.*}。
 * {@link MapperScan} 仅精确扫描 Mapper 包，避免把 Service 接口误注册成 Mapper Bean。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.inventory")
@EnableAsync
@EnableScheduling
@MapperScan({
        "com.inventory.modules.goods.product.mapper",
        "com.inventory.modules.goods.category.mapper",
        "com.inventory.modules.goods.brand.mapper",
        "com.inventory.modules.system.user.mapper",
        "com.inventory.modules.system.role.mapper",
        "com.inventory.modules.system.permission.mapper",
        "com.inventory.modules.system.log.mapper",
        "com.inventory.modules.monitor.apimonitor.mapper",
        "com.inventory.modules.shop.product.mapper",
        "com.inventory.modules.shop.records.mapper",
        "com.inventory.modules.order.orderinfo.mapper",
        "com.inventory.modules.invertory.stock.mapper"
})
public class PlatformServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }
}
