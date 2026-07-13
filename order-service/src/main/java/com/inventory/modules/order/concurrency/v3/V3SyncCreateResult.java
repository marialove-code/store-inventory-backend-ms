package com.inventory.modules.order.concurrency.v3;

import com.inventory.common.client.dto.LockStockFlowContext;
import lombok.Builder;
import lombok.Getter;

/**
 * V3 同步核心路径执行成功后的结果，供主线程立即返回 & 异步任务使用。
 */
@Getter
@Builder
public class V3SyncCreateResult {

    /** 已落库的订单号 */
    private final String orderNo;

    /** 下单用户名（压测时如 junit-t0，用于异步日志/模拟通知） */
    private final String userName;

    /** 写流水用的锁定库存上下文（来自 inventory-common） */
    private final LockStockFlowContext flowContext;
}
