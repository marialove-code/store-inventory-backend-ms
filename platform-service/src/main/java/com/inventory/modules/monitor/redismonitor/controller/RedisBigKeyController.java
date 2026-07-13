package com.inventory.modules.monitor.redismonitor.controller;
import com.inventory.common.response.Result;
import com.inventory.modules.monitor.redismonitor.entity.BigKeyItem;
import com.inventory.modules.monitor.redismonitor.service.RedisBigKeyService;
import com.inventory.modules.monitor.redismonitor.task.RedisBigKeyCollectTask;
import com.inventory.modules.monitor.redismonitor.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redis 大 Key 接口
 *
 * 分页返回缓存的 Top N 大 Key
 */
@RestController
@RequestMapping("/monitor/redis")
@RequiredArgsConstructor
public class RedisBigKeyController {

    private final RedisBigKeyService redisBigKeyService;

    private final RedisBigKeyCollectTask bigKeyTask;

    /**
     * 分页获取 Redis 大 Key
     *
     * @param page 当前页，从1开始
     * @param size 每页条数
     */
    @GetMapping("/bigkey")
    public Result<PageResult<BigKeyItem>> bigKey(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return Result.success(
                redisBigKeyService.getBigKeyPage(page, size)
        );
    }


   /* *//**
     * 手动触发大Key采集
     *//*
    @GetMapping("/collect")
    public String triggerBigKey() {
        bigKeyTask.collectOnce();
        return "大Key缓存已更新";
    }*/
}
