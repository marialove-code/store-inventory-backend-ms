package com.inventory.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 流控规则。
 * <p>
 * {@link FlowRuleManager#loadRules} 会整体覆盖，故探活与业务规则在此一并加载。
 * </p>
 */
@Slf4j
@Component
public class SentinelFlowRuleConfig {

    /** @deprecated 请用 {@link SentinelResourceNames#ORDER_PING} */
    public static final String RESOURCE_ORDER_PING = SentinelResourceNames.ORDER_PING;

    @Value("${app.sentinel.order-ping-flow-enabled:true}")
    private boolean orderPingFlowEnabled;

    /** 真实业务入口流控：下单 / 取消 */
    @Value("${app.sentinel.order-business-flow-enabled:true}")
    private boolean orderBusinessFlowEnabled;

    @Value("${app.sentinel.order-create-qps:20}")
    private double orderCreateQps;

    @Value("${app.sentinel.order-cancel-qps:20}")
    private double orderCancelQps;

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>(4);

        if (orderPingFlowEnabled) {
            rules.add(qpsRule(SentinelResourceNames.ORDER_PING, 2));
        } else {
            log.warn("【Sentinel】未加载 orderPing 流控（app.sentinel.order-ping-flow-enabled=false）");
        }

        if (orderBusinessFlowEnabled) {
            rules.add(qpsRule(SentinelResourceNames.ORDER_CREATE, orderCreateQps));
            rules.add(qpsRule(SentinelResourceNames.ORDER_CANCEL, orderCancelQps));
        } else {
            log.warn("【Sentinel】未加载业务流控 orderCreate/orderCancel（order-business-flow-enabled=false）");
        }

        FlowRuleManager.loadRules(rules);
        log.info("【Sentinel】已加载流控规则 {} 条：{}", rules.size(),
                rules.stream().map(r -> r.getResource() + "(QPS=" + r.getCount() + ")").toList());
    }

    private static FlowRule qpsRule(String resource, double qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        return rule;
    }
}
