package com.inventory.modules.monitor.apimonitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiPerfItemVo {

    private String id;

    private String apiPath;

    private Long requestCount;

    private Long successCount;

    private Long failCount;

    private Double avgTime;

    private String status;

    private LocalDateTime lastAccessTime;
}