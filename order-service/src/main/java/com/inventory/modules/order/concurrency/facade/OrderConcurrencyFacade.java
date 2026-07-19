package com.inventory.modules.order.concurrency.facade;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单并发实验统一门面。
 * <p>
 * 根据 URL 参数 {@code version=v1~v7 / v5r} 路由到对应 {@link OrderCreateConcurrencyStrategy} 实现，
 * 供压测与文档对比使用，不影响正式 {@code POST /order/info/add} 入口（除非后续主动合并）。
 * </p>
 */
@Service
public class OrderConcurrencyFacade {

    /** version 字符串 → 策略实现，Spring 启动时自动收集所有 Strategy Bean */
    private final Map<String, OrderCreateConcurrencyStrategy> strategyMap;

    /**
     * 注入所有 {@link OrderCreateConcurrencyStrategy} 实现类（V1～V7 各一个 @Component）。
     *
     * @param strategies Spring 容器中的策略列表
     */
    public OrderConcurrencyFacade(List<OrderCreateConcurrencyStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        OrderCreateConcurrencyStrategy::version,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("存在重复的并发版本实现: " + a.version());
                        }
                ));
    }

    /**
     * 按版本执行「创建订单 + 锁库存」实验逻辑。
     *
     * @param version 版本号，如 v1、v5；忽略大小写
     * @param dto     下单入参，与正式接口一致
     * @return 对应版本的处理结果
     */
    public Result<?> createOrder(String version, OrderInfoDTO dto) {
        ConcurrencyVersion concurrencyVersion = ConcurrencyVersion.fromCode(version);
        if (concurrencyVersion == null) {
            return Result.fail("不支持的并发版本: " + version + "，可选: v1~v7、v5r");
        }

        OrderCreateConcurrencyStrategy strategy = strategyMap.get(concurrencyVersion.getCode());
        if (strategy == null) {
            return Result.fail("版本 " + version + " 尚未注册 Spring Bean，请检查 v1~v7 / v5r 实现类");
        }

        return strategy.createOrder(dto);
    }

    /**
     * 返回当前已注册的版本列表（便于调试 / Swagger 说明）。
     */
    public Result<List<String>> listVersions() {
        List<String> versions = strategyMap.keySet().stream().sorted().toList();
        return Result.success(versions);
    }
}
