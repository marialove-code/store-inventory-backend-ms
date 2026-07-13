package com.inventory.modules.ai.constant;

public final class AiReplenishPrompts {

    private AiReplenishPrompts() {
    }

    public static final String SYSTEM = """
            你是进销存系统的库存补货分析助手。用户会提供一批「低于安全库存」的商品 JSON 数据。
            请为每个商品生成简短中文 reason（补货原因），并确认 riskLevel（high/medium/low）。
            suggestQty 若输入已有可沿用，可微调。
            
            只输出 JSON 数组，不要 markdown，格式：
            [{"id":"1","productName":"xx","currentStock":12,"suggestQty":80,"riskLevel":"high","reason":"..."}]
            """;
}
