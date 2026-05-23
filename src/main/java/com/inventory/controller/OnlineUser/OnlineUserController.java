package com.inventory.controller.OnlineUser;

import com.inventory.annotation.RequiresPerm;
import com.inventory.common.result.Result;
import com.inventory.entity.online.OnlineUser;
import com.inventory.entity.redis.RedisKeyVO;
import com.inventory.entity.redis.RedisMonitorVO;
import com.inventory.service.online.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Set;

/**
 * 在线用户监控控制器
 * 功能：查看在线用户列表、强制用户下线
 */
@RestController
@RequestMapping("/sys/online")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * 查询在线用户列表（分页 + 搜索）
     * 接口地址：GET /sys/online/list
     * 权限标识：system:online:list
     */
    @GetMapping("/list")
    @RequiresPerm("system:online:list")
    public Result<Page<OnlineUser>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        // 直接返回和你用户管理一样的 Page 结构
        Page<OnlineUser> page = onlineUserService.pageOnlineUser(keyword, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 强制用户下线（删除Redis中的登录Token）
     * 接口地址：DELETE /sys/online/{tokenKey}
     * 权限标识：system:online:forceLogout
     */
    @DeleteMapping("/{tokenKey}")
    @RequiresPerm("system:online:forceLogout")
    public Result<Void> forceLogout(@PathVariable String tokenKey) {
        boolean success = onlineUserService.forceLogout(tokenKey);
        if (success) {
            return Result.success();
        } else {
            return Result.fail("用户已下线或会话不存在");
        }
    }


    @GetMapping("/redis/info")
    public Result<RedisMonitorVO> getRedisInfo(){
        return Result.success(onlineUserService.getRedisMonitorInfo());
    }

    @GetMapping("/redis/page")
    @RequiresPerm("system:online:redis:list")    // 对应 ONLINE_PERM.REDIS_LIST
    public Result<Page<RedisKeyVO>> pageRedisKey(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize
    ) {
        Page<RedisKeyVO> page = onlineUserService.pageRedisKey(keyword, pageNum, pageSize);
        return Result.success(page);
    }

    @DeleteMapping("/redis/del")
    @RequiresPerm("system:online:redis:delete")  // 对应 ONLINE_PERM.REDIS_DELETE
    public Result<Boolean> delKey(@RequestParam String key){
        return Result.success(onlineUserService.deleteRedisKey(key));
    }
}