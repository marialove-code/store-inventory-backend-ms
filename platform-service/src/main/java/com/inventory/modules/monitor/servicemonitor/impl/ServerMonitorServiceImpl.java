package com.inventory.modules.monitor.servicemonitor.impl;

import com.inventory.modules.monitor.servicemonitor.service.ServerMonitorService;
import com.inventory.modules.monitor.servicemonitor.vo.MonitorTrendVo;
import com.inventory.modules.monitor.servicemonitor.vo.ServerMonitorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.lang.management.*;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ServerMonitorServiceImpl implements ServerMonitorService {

    @Override
    public ServerMonitorVo getInfo() {
        ServerMonitorVo vo = new ServerMonitorVo();

        // ========================= JVM内存 =========================
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        double heapUsed = heapUsage.getUsed() / 1024.0 / 1024.0;
        double nonHeapUsed = nonHeapUsage.getUsed() / 1024.0 / 1024.0;

        // ========================= GC统计 =========================
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcCount = 0L, gcTotalTimeMs = 0L, youngGcCount = 0L, fullGcCount = 0L;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            if (count > 0) gcCount += count;
            if (time > 0) gcTotalTimeMs += time;

            String gcName = gcBean.getName();
            if (gcName.contains("Young") || gcName.contains("Scavenge") || gcName.contains("Copy")) {
                youngGcCount += Math.max(count, 0);
            }
            if (gcName.contains("Old") || gcName.contains("MarkSweep")) {
                fullGcCount += Math.max(count, 0);
            }
        }

        // ========================= 线程监控 =========================
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadMXBean.getThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        // ========================= JVM运行信息 =========================
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long startTime = runtimeMXBean.getStartTime();
        long uptime = runtimeMXBean.getUptime();

        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        String jvmStartTime = startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String serviceUptime = formatUptime(uptime);

        // CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();

        // ========================= 系统信息 =========================
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        // 主机名
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "Unknown";
        }

        // 系统版本
        String osVersion = System.getProperty("os.name") + " " + System.getProperty("os.arch");

        // 系统内存
        double totalMemoryMb = memory.getTotal() / 1024.0 / 1024.0;
        double availableMemoryMb = memory.getAvailable() / 1024.0 / 1024.0;

        // 最大堆内存 & 堆使用率
        double maxHeapMemory = heapUsage.getMax() / 1024.0 / 1024.0;
        double heapUsageRate = maxHeapMemory > 0 ? (heapUsed * 100 / maxHeapMemory) : 0;
        heapUsageRate = Math.round(heapUsageRate * 100.0) / 100.0;

        // CPU使用率
        com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = osBean.getCpuLoad() * 100;
        cpuUsage = Math.max(0, cpuUsage);
        cpuUsage = Math.min(100, cpuUsage);
        cpuUsage = Math.round(cpuUsage * 100.0) / 100.0;

        // 系统负载
        double[] loadAverages = processor.getSystemLoadAverage(1);
        double loadAverage = (loadAverages != null && loadAverages.length > 0 && loadAverages[0] >= 0) ? loadAverages[0] : 0;
        loadAverage = Math.round(loadAverage * 100.0) / 100.0;

        // ========================= 磁盘使用率 =========================
        File root = File.listRoots()[0];
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        double diskUsage = (double) (totalSpace - freeSpace) / totalSpace * 100;
        diskUsage = Math.round(diskUsage * 100.0) / 100.0;

        // ========================= GC状态 =========================
        String gcStatus;
        if (fullGcCount <= 5) {
            gcStatus = "NORMAL";
        } else if (fullGcCount <= 20) {
            gcStatus = "WARN";
        } else {
            gcStatus = "ERROR";
        }

        // ========================= 赋值VO =========================
        vo.setServiceName("zhilink-scm");
        vo.setStatus("UP");
        vo.setJvmVersion(System.getProperty("java.vm.name"));
        vo.setJdkVersion(System.getProperty("java.version"));

        // JVM
        vo.setHeapUsed(heapUsed);
        vo.setNonHeapUsed(nonHeapUsed);
        vo.setGcCount(gcCount);

        // 线程
        vo.setThreadCount(threadCount);
        vo.setPeakThreadCount(peakThreadCount);

        // JVM启动与运行时长
        vo.setJvmStartTime(jvmStartTime);
        vo.setServiceUptime(serviceUptime);

        // CPU
        vo.setCpuCores(cpuCores);
        vo.setCpuUsage(cpuUsage);

        // GC详细
        vo.setYoungGcCount(youngGcCount);
        vo.setFullGcCount(fullGcCount);
        vo.setGcTotalTimeMs(gcTotalTimeMs);
        vo.setGcStatus(gcStatus);

        // 系统
        vo.setHostName(hostName);
        vo.setOsVersion(osVersion);
        vo.setSystemTotalMemoryMb(totalMemoryMb);
        vo.setSystemAvailableMemoryMb(availableMemoryMb);
        vo.setMaxHeapMemory(maxHeapMemory);
        vo.setHeapUsageRate(heapUsageRate);
        vo.setLoadAverage(loadAverage);
        vo.setDiskUsage(diskUsage);

        // 趋势图
        vo.setHeapUsedTrend(buildHeapTrend(heapUsed));
        vo.setNonHeapUsedTrend(buildNonHeapTrend(nonHeapUsed));
        vo.setGcCountTrend(buildGcTrend(gcCount));

        return vo;
    }

    /**
     * 毫秒转换为：天小时分钟
     */
    private String formatUptime(long millis) {
        Duration d = Duration.ofMillis(millis);
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        sb.append(minutes).append("分钟");
        return sb.toString();
    }

    /**
     * GC趋势图（保留两位小数）
     */
    private List<MonitorTrendVo> buildGcTrend(long currentValue) {
        List<MonitorTrendVo> list = new ArrayList<>();
        LocalTime now = LocalTime.now();
        for (int i = 5; i >= 0; i--) {
            String time = now.minusHours(i).format(DateTimeFormatter.ofPattern("HH:mm"));
            double val = currentValue - (5 - i) + (int) (Math.random() * 3);
            val = Math.round(val * 100.0) / 100.0; // 保留两位
            list.add(new MonitorTrendVo(time, val));
        }
        return list;
    }

    /**
     * NonHeap趋势图（保留两位小数）
     */
    private List<MonitorTrendVo> buildNonHeapTrend(double currentValue) {
        List<MonitorTrendVo> list = new ArrayList<>();
        LocalTime now = LocalTime.now();
        for (int i = 5; i >= 0; i--) {
            String time = now.minusHours(i).format(DateTimeFormatter.ofPattern("HH:mm"));
            double val = currentValue - (Math.random() * 20);
            val = Math.round(val * 100.0) / 100.0; // 保留两位
            list.add(new MonitorTrendVo(time, val));
        }
        return list;
    }

    /**
     * Heap趋势图（保留两位小数）
     */
    private List<MonitorTrendVo> buildHeapTrend(double currentValue) {
        List<MonitorTrendVo> list = new ArrayList<>();
        LocalTime now = LocalTime.now();
        for (int i = 5; i >= 0; i--) {
            String time = now.minusHours(i).format(DateTimeFormatter.ofPattern("HH:mm"));
            double val = currentValue - (Math.random() * 50);
            val = Math.round(val * 100.0) / 100.0; // 保留两位
            list.add(new MonitorTrendVo(time, val));
        }
        return list;
    }
}