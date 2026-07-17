package com.inventory.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.inventory.common.response.Result;
import com.inventory.order.config.SentinelResourceNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sentinel 熔断演示（学习用）。
 * <p>
 * {@code fail=true} 模拟下游失败（抛异常，计入异常比例）；
 * 连续失败触发熔断后，即使 {@code fail=false} 也会走 {@link #unstableBlockHandler}。
 * </p>
 */
@RestController
@RequestMapping("/order")
public class OrderDegradeDemoController {

    /**
     * 例：连续 5 次以上 {@code ?fail=true}，再打 {@code ?fail=false} 应被熔断拦住。
     */
    @GetMapping("/demo/unstable")
    @SentinelResource(
            value = SentinelResourceNames.ORDER_UNSTABLE,
            blockHandler = "unstableBlockHandler"
    )
    public Result<Map<String, String>> unstable(
            @RequestParam(defaultValue = "false") boolean fail) {
        if (fail) {
            // 业务/下游异常：计入熔断统计；未熔断时会进全局异常处理
            throw new IllegalStateException("模拟下游调用失败（学习用）");
        }
        Map<String, String> data = new LinkedHashMap<>(3);
        data.put("resource", SentinelResourceNames.ORDER_UNSTABLE);
        data.put("status", "OK");
        data.put("message", "下游正常，未触发熔断");
        return Result.success(data);
    }

    /**
     * 熔断打开后：请求不再进方法体，直接到这里（BlockException）。
     */
    public Result<Map<String, String>> unstableBlockHandler(boolean fail, BlockException ex) {
        return Result.fail("触发 Sentinel 熔断（资源 orderUnstable），请稍后再试："
                + ex.getClass().getSimpleName() + "（本次 fail=" + fail + "）");
    }
}
