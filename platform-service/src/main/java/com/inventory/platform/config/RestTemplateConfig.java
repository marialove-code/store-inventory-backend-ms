package com.inventory.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 跨服务 HTTP 客户端配置。
 * <p>
 * 本阶段不使用 Feign / Nacos，平台通过 {@link RestTemplate} 调用 inventory-service
 * 内部接口（如 init-stock、usable）。超时设置避免远端卡住拖死平台线程。
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 注册 RestTemplate Bean。
     * <p>
     * 连接超时 3 秒、读取超时 10 秒，与订单服务客户端策略对齐。
     * </p>
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
