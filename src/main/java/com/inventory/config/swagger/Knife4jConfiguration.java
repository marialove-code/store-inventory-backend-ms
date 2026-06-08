package com.inventory.config.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Knife4j（OpenAPI3）文档基础信息配置。
 *
 * @author inventory
 */
@Configuration
public class Knife4jConfiguration {

    @Value("${app.version:V1.0}")
    private String appVersion;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("门店简易记账 · 后端 API 文档")
                        .description(appVersion + " 稳定版 — 门店开单、进销存、订单、RBAC 权限")
                        .version(appVersion)
                        .contact(new Contact().name("inventory").email("dev@example.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
