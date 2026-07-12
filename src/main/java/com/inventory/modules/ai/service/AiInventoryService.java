package com.inventory.modules.ai.service;

import com.inventory.modules.ai.vo.AiReplenishItemVO;

import java.util.List;

/**
 * AI 库存补货建议服务。
 * <p>
 * <b>为什么用：</b>预警页需要「可执行的补货建议」，纯规则不够自然，LLM 可生成原因说明；
 * 但数量建议以数据库真实库存+预警阈值为底，避免模型编造数字。
 * </p>
 * <p>
 * <b>是否连库：</b>是。查询 {@code inventory_stock} 中 {@code stock < stock_warn} 的记录。
 * </p>
 */
public interface AiInventoryService {

    /**
     * 基于真实低库存数据生成 AI 补货建议列表。
     *
     * @return 补货条目，无预警商品时返回空列表
     */
    List<AiReplenishItemVO> replenishAdvice();
}
