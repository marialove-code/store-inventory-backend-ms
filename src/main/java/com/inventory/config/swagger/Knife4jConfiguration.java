package com.inventory.config.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Knife4j（OpenAPI3）文档基础信息配置。
 * <p>
 * 配置了 Bearer JWT 后，Knife4j 文档页会出现「Authorize / 授权」入口，
 * 调试需登录接口时不必每个接口手动填 Authorization 头。
 * </p>
 *
 * @author inventory
 */
@Configuration
public class Knife4jConfiguration {

    /** 与 {@code jwt.header} 一致，OpenAPI 安全方案名称 */
    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Value("${app.version:v1.0}")
    private String appVersion;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("门店简易记账 · 后端 API 文档")
                        .description(appVersion + " 稳定版 — 门店开单、进销存、订单、RBAC 权限")
                        .version(appVersion)
                        .contact(new Contact().name("inventory").email("dev@example.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                // 默认所有接口文档带上「需要 Authorization」标记（登录等白名单接口仍可不带 Token 调试）
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("先调 POST /auth/login 获取 token，在此填入（只需 token 本体，不要加 Bearer 前缀）")));
    }
}
