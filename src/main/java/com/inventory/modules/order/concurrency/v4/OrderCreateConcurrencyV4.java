package com.inventory.modules.order.concurrency.v4;

import com.inventory.common.response.Result;
import com.inventory.modules.order.concurrency.common.ConcurrencyVersion;
import com.inventory.modules.order.concurrency.strategy.OrderCreateConcurrencyStrategy;
import com.inventory.modules.order.orderinfo.dto.OrderInfoDTO;
import org.springframework.stereotype.Component;

/**
 * V4：SQL 并发控制（库存安全）。
 * <p>
 * <b>计划实现</b>（待开发，二选一或对比两种）：
 * <ul>
 *   <li><b>乐观锁</b>：{@code inventory_stock} 表增加 {@code version} 字段，
 *       {@code UPDATE ... WHERE goods_id=? AND version=?}，失败则重试或返回库存冲突</li>
 *   <li><b>悲观锁</b>：{@code SELECT ... FOR UPDATE} 在同一事务内锁定库存行后再更新 lockStock</li>
 * </ul>
 * </p>
 * <p>
 * <b>压测目标</b>：200 并发下成功订单数 ≤ 100，lockStock 不超过 stock。
 * </p>
 */
@Component
public class OrderCreateConcurrencyV4 implements OrderCreateConcurrencyStrategy {

    @Override
    public String version() {
        return ConcurrencyVersion.V4.getCode();
    }

    /**
     * TODO: 在 inventory_stock 上实现乐观锁或悲观锁更新 lockStock。
     */
    @Override
    public Result<?> createOrder(OrderInfoDTO dto) {
        return Result.fail("V4 待实现：SQL 乐观锁 / 悲观锁");
    }
}
