package com.inventory.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Sentinel 流控规则（学习用）。
 * <p>
 * 资源名 {@code orderPing} 与 {@code OrderPingController} 上 {@code @SentinelResource} 一致。
 * 通过 {@code app.sentinel.order-ping-flow-enabled} 开关对比：
 * <ul>
 *   <li>true：加载 QPS=2 规则 → 连点会限流</li>
 *   <li>false：清空规则 → 怎么打都不限流（体会「没规则 = 不拦截」）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class SentinelFlowRuleConfig {

    public static final String RESOURCE_ORDER_PING = "orderPing";

    /**
     * 学习开关：是否为 orderPing 加载流控规则。
     */
    @Value("${app.sentinel.order-ping-flow-enabled:true}")
    private boolean orderPingFlowEnabled;

    @PostConstruct
    public void initFlowRules() {
        if (!orderPingFlowEnabled) {
            FlowRuleManager.loadRules(Collections.emptyList());
            log.warn("【Sentinel】已关闭 orderPing 流控（app.sentinel.order-ping-flow-enabled=false），连点也不会限流");
            return;
        }

        FlowRule rule = new FlowRule();
        rule.setResource(RESOURCE_ORDER_PING);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 每秒最多 2 次；超过则 BlockException → blockHandler
        rule.setCount(2);
        FlowRuleManager.loadRules(Collections.singletonList(rule));
        log.info("【Sentinel】已加载流控规则 resource={}, QPS={}", RESOURCE_ORDER_PING, rule.getCount());
    }
}
