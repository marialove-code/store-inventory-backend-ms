package com.inventory.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 熔断规则。
 * <p>
 * {@link DegradeRuleManager#loadRules} 会整体覆盖，故演示与业务规则在此一并加载。
 * 业务侧挂在 Feign 门面：{@code inventoryLock} / {@code inventoryUnlock}。
 * </p>
 */
@Slf4j
@Component
public class SentinelDegradeRuleConfig {

    /** @deprecated 请用 {@link SentinelResourceNames#ORDER_UNSTABLE} */
    public static final String RESOURCE_ORDER_UNSTABLE = SentinelResourceNames.ORDER_UNSTABLE;

    @Value("${app.sentinel.order-unstable-degrade-enabled:true}")
    private boolean orderUnstableDegradeEnabled;

    /** 真实业务：锁/解锁库存熔断 */
    @Value("${app.sentinel.inventory-degrade-enabled:true}")
    private boolean inventoryDegradeEnabled;

    @PostConstruct
    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>(4);

        if (orderUnstableDegradeEnabled) {
            rules.add(exceptionRatioRule(SentinelResourceNames.ORDER_UNSTABLE));
        } else {
            log.warn("【Sentinel】未加载 orderUnstable 熔断（order-unstable-degrade-enabled=false）");
        }

        if (inventoryDegradeEnabled) {
            rules.add(exceptionRatioRule(SentinelResourceNames.INVENTORY_LOCK));
            rules.add(exceptionRatioRule(SentinelResourceNames.INVENTORY_UNLOCK));
            // 下单会先查可用库存；不挂的话停 inventory 时永远进不了 lock，熔断演示不到
            rules.add(exceptionRatioRule(SentinelResourceNames.INVENTORY_USABLE));
        } else {
            log.warn("【Sentinel】未加载 inventoryLock/Unlock/Usable 熔断（inventory-degrade-enabled=false）");
        }

        DegradeRuleManager.loadRules(rules);
        log.info("【Sentinel】已加载熔断规则 {} 条（异常比例≥50%，minRequest=5，timeWindow=10s）", rules.size());
    }

    private static DegradeRule exceptionRatioRule(String resource) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(0.5D);
        rule.setMinRequestAmount(5);
        rule.setTimeWindow(10);
        rule.setStatIntervalMs(10_000);
        return rule;
    }
}
