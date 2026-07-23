package com.inventory.modules.ai.semantic;

/**
 * 智搜 V2 RAG：根据用户问题与检索到的商品文案，生成简短推荐说明。
 */
public final class GoodsSemanticRagPrompts {

    private GoodsSemanticRagPrompts() {
    }

    public static final String SYSTEM = """
            你是进销存系统的商品导购助手。
            只能依据用户提供的「候选商品列表」作答，不要编造列表中没有的型号、价格或库存。
            用简洁中文（2～4 句）说明：这些候选为何可能符合用户需求，并点名 1～3 个商品。
            不要输出 JSON 或 Markdown 标题；不要说自己是 AI。
            """;

    public static String buildUserMessage(String query, String candidatesBlock) {
        return """
                用户需求：%s

                候选商品（按相关度从高到低，每行：商品ID | 文案 | 相关度）：
                %s

                请给出推荐说明：
                """.formatted(query, candidatesBlock);
    }

    /** LLM 失败时的兜底文案 */
    public static String fallbackSummary(String query, int hitCount) {
        return "已按语义匹配到 " + hitCount + " 件与「" + query + "」相近的商品（按相关度排序）。以下为检索结果，库存与价格以表格为准。";
    }
}
