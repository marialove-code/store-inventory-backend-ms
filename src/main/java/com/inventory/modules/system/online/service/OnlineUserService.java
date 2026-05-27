package com.inventory.modules.system.online.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.modules.system.online.entity.OnlineUser;
import com.inventory.modules.system.monitor.vo.RedisKeyVO;
import com.inventory.modules.system.monitor.vo.RedisMonitorVO;

import java.util.Set;

/**
 * 在线用户 Service 接口
 */
public interface OnlineUserService {

    /**
     * 分页查询在线用户（从Redis读取）
     * @param keyword 搜索关键词（用户名/IP）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    Page<OnlineUser> pageOnlineUser(String keyword, Long pageNum, Long pageSize);

    /**
     * 强制下线（删除Redis中的token）
     * @param tokenKey redis的key
     * @return 是否成功
     */
    boolean forceLogout(String tokenKey);

    /**
     * 获取Redis监控基础信息
     */
    RedisMonitorVO getRedisMonitorInfo();

    /**
     * 根据key模糊搜索redis键
     */
    Set<String> searchRedisKey(String keyword);

    /**
     * 删除指定redis key
     */
    Boolean deleteRedisKey(String key);

    /**
     * 查询 redis key
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<RedisKeyVO> pageRedisKey(String keyword, Long pageNum, Long pageSize);
}