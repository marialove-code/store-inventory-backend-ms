package com.inventory.modules.ai.service;

import com.inventory.modules.ai.vo.AiOpsLogItemVO;

import java.util.List;

/**
 * AI 运维助手：分析系统操作日志。
 * <p>
 * <b>是否连库：</b>是。读取 {@code sys_operation_log} 最近记录。
 * </p>
 */
public interface AiOpsService {

    List<AiOpsLogItemVO> analyzeRecent();
}
