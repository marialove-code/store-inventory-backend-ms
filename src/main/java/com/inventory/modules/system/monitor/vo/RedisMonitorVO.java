package com.inventory.modules.system.monitor.vo;

import lombok.Data;

@Data
public class RedisMonitorVO {
    // 连接状态
    private String connectStatus;
    // 在线用户数
    private Long onlineUserCount;
    // 内存占用
    private String usedMemory;
    // 全局key总数
    private Long totalKey;
}