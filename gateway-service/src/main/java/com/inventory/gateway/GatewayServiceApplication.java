package com.inventory.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类。
 * <p>
 * 统一对外入口（默认 8080），按路径前缀将请求转发到 Nacos 中的业务服务：
 * inventory / order / ai / platform。前端只需打网关，不必再记四个端口。
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
