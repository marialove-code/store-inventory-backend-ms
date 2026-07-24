package com.inventory.modules.ai.learning.reference;

/**
 * 【学习用 · 非生产 Bean】LangChain「Tools / Agent」↔ Spring AI 对照（规划 Step A，当前未接入生产）。
 * <p>
 * <b>LangChain Agent 在干什么？</b>
 * 模型不直接瞎编答案，而是决定「要不要调工具」→ 执行 Python/Java 函数 → 把结果塞回对话 → 再总结。
 * </p>
 * <pre>
 * # LangChain 概念（Python）
 * @tool
 * def get_stock(goods_id: int) -> str:
 *     """查询商品可用库存"""
 *     ...
 *
 * agent = create_tool_calling_agent(llm, tools=[get_stock], prompt)
 * executor = AgentExecutor(agent=agent, tools=tools)
 * executor.invoke({"input": "商品 1001 还能卖吗？"})
 * </pre>
 * <p>
 * <b>Spring AI 等价思路（Java，Spring AI 1.0 M6 方向）：</b>
 * </p>
 * <pre>
 * // 1. 定义工具：名称、描述、参数 schema、执行逻辑
 * FunctionCallback.builder()
 *     .function("getStock", "根据商品ID查询可用库存", request -> { ... })
 *     .inputType(StockQuery.class)
 *     .build();
 *
 * // 2. 注册到 ChatClient / ChatModel 的 function callbacks
 * // 3. 模型返回 tool_calls → 框架执行函数 → 把 ToolResponseMessage 追加进 messages → 再 call 一次
 * </pre>
 * <p>
 * <b>本项目为什么还没做？</b>
 * <ul>
 *   <li>客服 MVP 刻意<strong>不查库</strong>，避免模型编造库存（见 AiChatPrompts）</li>
 *   <li>Step A 计划：只读工具 1～2 个（如库存快照、订单状态），白名单 + 鉴权</li>
 *   <li>Step B 才是「极简 Agent」多步；不做 LangChain 重型 AgentExecutor</li>
 * </ul>
 * </p>
 * <p>
 * <b>和 RAG 的区别（面试常问）：</b>
 * <ul>
 *   <li><b>RAG</b>：检索<strong>文本片段</strong>进 Prompt（我们智搜 V2 已做）</li>
 *   <li><b>Tool</b>：调<strong>实时 API</strong>拿结构化结果（库存数字以 DB 为准）</li>
 * </ul>
 * </p>
 */
public final class ToolCallingReference {

    private ToolCallingReference() {
    }

    /**
     * 工具定义示例（伪代码注释，不连接 Feign）。
     * <p>
     * 生产落地时建议放在 {@code ai-service}，通过 Feign 调 {@code inventory-service}，
     * 且只允许 GET 只读接口。
     * </p>
     */
    public interface StockTool {
        /**
         * LangChain @tool 的 docstring 会传给模型，帮助它决定何时调用。
         * Spring AI 用 function description 字段，作用相同。
         *
         * @param goodsId 商品主键
         * @return 例如 "可用库存=12"（必须来自数据库，禁止模型编造）
         */
        String getUsableStock(Long goodsId);
    }

    /**
     * Agent 多步循环（概念，LangChain AgentExecutor.max_iterations=3）。
     * <pre>
     * while (step &lt; maxSteps) {
     *     response = chatModel.call(messages);
     *     if (response 无 tool_calls) return 最终答案;
     *     for (toolCall : response.tool_calls) {
     *         result = 执行对应 Tool;
     *         messages.add(ToolResponseMessage);
     *     }
     * }
     * </pre>
     */
    public static void agentLoopPseudocode() {
        // 实现留作 Step B；复习时能说清循环即可
    }

    /**
     * 安全护栏（比链本身更重要）：
     * <ol>
     *   <li>工具白名单：仅 inventory.getUsableStock、order.getStatus</li>
     *   <li>参数校验：goodsId 必须为正 Long</li>
     *   <li>鉴权：沿用 JWT，工具层不信任模型传的 userId</li>
     *   <li>超时：单次 tool 调用 &lt; 3s</li>
     *   <li>失败降级：Tool 异常 → 提示用户去页面查看</li>
     * </ol>
     */
    public static String safetyChecklist() {
        return "见本方法 JavaDoc";
    }
}
