package com.inventory.order.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Nacos Config 演示配置。
 * <p>
 * 使用 {@code @Value} + {@link RefreshScope}：Nacos 中 {@code order-service.yaml}
 * 覆盖 {@code app.nacos-demo-message} 后可热刷新（不必重启）。
 * </p>
 */
@Getter
@Component
@RefreshScope
public class OrderNacosDemoProperties {

    /**
     * 演示用文案：未拉到远程时为 local-default；拉到则为 Nacos 中的值。
     */
    @Value("${app.nacos-demo-message:local-default}")
    private String nacosDemoMessage;
}
