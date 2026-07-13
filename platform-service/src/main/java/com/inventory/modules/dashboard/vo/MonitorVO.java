package com.inventory.modules.dashboard.vo;

import lombok.Data;

@Data
public class MonitorVO {
    private String redisStatus;      // 状态：正常/异常
    private Integer redisMemory;     // 内存使用率 %
    private Integer onlineUser;      // 在线人数
    private Long apiCount;           // 今日接口调用量
}