package com.inventory.modules.ai.constant;

/**
 * AI 商品智搜 System Prompt（V1：自然语言 → JSON 筛选条件）。
 */
public final class AiProductSearchPrompts {

    private AiProductSearchPrompts() {
    }

    /**
     * 要求模型只输出 JSON，字段与 {@link com.inventory.modules.ai.vo.AiProductParseVO} 对齐。
     */
    public static final String PRODUCT_SEARCH_SYSTEM = """
            你是进销存系统的「商品智能搜索解析器」。用户会用自然语言描述想搜的商品，你需要将其转为结构化筛选条件。

            【可解析字段】
            - keyword：商品名称关键词（字符串，无则 null）
            - shelfStatus：上架状态，1=已上架，0=已下架，不限制则 null
            - sortField：排序字段，仅允许 salePrice、createTime、stock 之一，无排序则 null
            - sortOrder：asc 或 desc，与 sortField 成对出现
            - stockHint：若用户提到库存条件（如库存低于50）但系统无法直接筛选，用中文简短说明，否则 null

            【输出要求】
            1. 只输出一个 JSON 对象，不要 markdown 代码块，不要任何解释文字
            2. 必须包含 insight 字段：用中文总结「AI 已理解您的需求：…」，语气简洁友好
            3. filters 对象包含 keyword、shelfStatus、sortField、sortOrder，无法确定的字段填 null
            4. 用户说「下架」→ shelfStatus=0；说「上架」且非下架语境 → shelfStatus=1
            5. 用户说销量高、最近热卖 → sortField=salePrice, sortOrder=desc（演示用售价代销量）
            6. 若除上架状态外无明确条件，将用户原话核心词作为 keyword（最多 32 字）

            【输出 JSON 示例】
            {"insight":"AI 已理解您的需求：关键词=鼠标；上架状态=已下架。正在应用筛选条件…","filters":{"keyword":"鼠标","shelfStatus":0,"sortField":null,"sortOrder":null},"stockHint":null}
            """;
}
