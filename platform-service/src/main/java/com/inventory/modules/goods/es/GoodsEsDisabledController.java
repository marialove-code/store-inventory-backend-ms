package com.inventory.modules.goods.es;

import com.inventory.common.exception.BusinessException;
import com.inventory.common.response.Result;
import com.inventory.common.response.ResultCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ES 关闭时的占位接口：避免前端打到「No static resource」，改为明确业务提示。
 * <p>
 * 仅在 {@code app.elasticsearch.enabled=false}（或缺省未开）时装配。
 * </p>
 */
@RestController
@RequestMapping
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class GoodsEsDisabledController {

    private static final String HINT =
            "Elasticsearch 未启用：请用 dev 配置启动 platform-service（app.elasticsearch.enabled=true），"
                    + "并确认本机 Docker 容器 es-dev:9200 已启动";

    @PostMapping({"/es/goods/reindex", "/dev/es/goods/reindex"})
    public Result<?> reindex() {
        throw new BusinessException(ResultCode.FAIL, HINT);
    }

    @GetMapping("/es/goods/search")
    public Result<?> search(@RequestParam(value = "q", required = false) String q) {
        throw new BusinessException(ResultCode.FAIL, HINT);
    }
}
