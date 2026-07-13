package com.inventory.modules.system.log.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.system.log.entity.SysOperationLog;
import com.inventory.modules.system.log.service.SysOperationLogService;
import com.inventory.modules.system.log.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/**
* @author 95349
* @description 针对表【sys_operation_log(系统操作审计日志表)】的数据库操作Service实现
* @createDate 2026-05-08 10:31:31
*/
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
    implements SysOperationLogService{

    private final SysOperationLogMapper sysOperationLogMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<SysOperationLog> pageList(String username, String title, String operationType, Short operateStatus,
                                          String beginTime, String endTime, Long pageNum, Long pageSize) {
        Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(SysOperationLog::getUsername, username);
        }
        if (StringUtils.hasText(title)) {
            wrapper.like(SysOperationLog::getTitle, title);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.like(SysOperationLog::getOperationType, operationType);
        }
        if (operateStatus != null) {
            wrapper.eq(SysOperationLog::getOperateStatus, operateStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            wrapper.ge(SysOperationLog::getCreateTime, LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(SysOperationLog::getCreateTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);
        return sysOperationLogMapper.selectPage(page, wrapper);
    }

    @Override
    public void exportExcel(HttpServletResponse response,
                            String username,
                            String title,
                            String operationType,
                            Short operateStatus,
                            String beginTime,
                            String endTime) throws IOException {

        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            queryWrapper.like(SysOperationLog::getUsername, username);
        }
        if (StringUtils.hasText(title)) {
            queryWrapper.like(SysOperationLog::getTitle, title);
        }
        if (StringUtils.hasText(operationType)) {
            queryWrapper.like(SysOperationLog::getOperationType, operationType);
        }
        if (operateStatus != null) {
            queryWrapper.eq(SysOperationLog::getOperateStatus, operateStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.ge(SysOperationLog::getCreateTime,
                    LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(SysOperationLog::getCreateTime,
                    LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }
        queryWrapper.orderByDesc(SysOperationLog::getCreateTime);
        List<SysOperationLog> originList = sysOperationLogMapper.selectList(queryWrapper);

        List<Map<String, Object>> exportDataList = originList.stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("username", log.getUsername());
            map.put("title", log.getTitle());
            map.put("operationType", log.getOperationType());
            map.put("requestMethod", log.getRequestMethod());
            map.put("requestUri", log.getRequestUri());
            map.put("ipAddress", log.getIpAddress());
            map.put("browser", log.getBrowser());
            map.put("operatingSystem", log.getOperatingSystem());
            map.put("requestParams", log.getRequestParams());
            map.put("operateStatus", Objects.equals(log.getOperateStatus(), 1) ? "成功" : "失败");
            map.put("errorMessage", log.getErrorMessage());
            // LocalDateTime 转字符串，和登录日志保持一致
            map.put("createTime", log.getCreateTime() == null
                    ? ""
                    : log.getCreateTime().format(DATE_TIME_FORMATTER));
            return map;
        }).toList();

        // ====================== 以下完全和你登录日志导出写法一致 ======================
        response.setBufferSize(8192);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");

        String fileName = "操作日志.xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);

        try (OutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {

            // 表头别名
            writer.addHeaderAlias("username", "操作账号");
            writer.addHeaderAlias("title", "操作模块");
            writer.addHeaderAlias("operationType", "操作类型");
            writer.addHeaderAlias("requestMethod", "请求方式");
            writer.addHeaderAlias("requestUri", "请求地址");
            writer.addHeaderAlias("ipAddress", "客户端IP");
            writer.addHeaderAlias("browser", "浏览器");
            writer.addHeaderAlias("operatingSystem", "操作系统");
            writer.addHeaderAlias("requestParams", "请求参数");
            writer.addHeaderAlias("operateStatus", "操作状态");
            writer.addHeaderAlias("errorMessage", "异常信息");
            writer.addHeaderAlias("createTime", "操作时间");

            // 只导出别名对应的列
            writer.setOnlyAlias(true);
            writer.write(exportDataList, true);

            writer.flush(out, false);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Page<SysOperationLog> pageListMy(String title, String operationType,
                                            Short operateStatus, String beginTime, String endTime,
                                            Long pageNum, Long pageSize) {
        // 获取当前登录用户
        LoginUserVO loginUser = LoginUserContext.getUser();
        String username = loginUser.getUsername();

        Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();

        // 只查当前用户
        queryWrapper.eq(SysOperationLog::getUsername, username);

        if (StringUtils.hasText(title)) {
            queryWrapper.like(SysOperationLog::getTitle, title);
        }
        if (StringUtils.hasText(operationType)) {
            queryWrapper.like(SysOperationLog::getOperationType, operationType);
        }
        if (operateStatus != null) {
            queryWrapper.eq(SysOperationLog::getOperateStatus, operateStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.ge(SysOperationLog::getCreateTime, LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(SysOperationLog::getCreateTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }

        queryWrapper.orderByDesc(SysOperationLog::getCreateTime);
        return sysOperationLogMapper.selectPage(page, queryWrapper);
    }

    @Override
    public void exportExcelMy(HttpServletResponse response, String title, String operationType,
                              Short operateStatus, String beginTime, String endTime) throws IOException {
        // 获取当前登录用户
        LoginUserVO loginUser = LoginUserContext.getUser();
        String username = loginUser.getUsername();

        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        // 只查当前用户
        queryWrapper.eq(SysOperationLog::getUsername, username);

        if (StringUtils.hasText(title)) {
            queryWrapper.like(SysOperationLog::getTitle, title);
        }
        if (StringUtils.hasText(operationType)) {
            queryWrapper.like(SysOperationLog::getOperationType, operationType);
        }
        if (operateStatus != null) {
            queryWrapper.eq(SysOperationLog::getOperateStatus, operateStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.ge(SysOperationLog::getCreateTime, LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(SysOperationLog::getCreateTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }

        queryWrapper.orderByDesc(SysOperationLog::getCreateTime);
        List<SysOperationLog> originList = sysOperationLogMapper.selectList(queryWrapper);

        List<Map<String, Object>> exportDataList = originList.stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("username", log.getUsername());
            map.put("title", log.getTitle());
            map.put("operationType", log.getOperationType());
            map.put("requestMethod", log.getRequestMethod());
            map.put("requestUri", log.getRequestUri());
            map.put("ipAddress", log.getIpAddress());
            map.put("browser", log.getBrowser());
            map.put("operatingSystem", log.getOperatingSystem());
            map.put("requestParams", log.getRequestParams());
            map.put("operateStatus", Objects.equals(log.getOperateStatus(), 1) ? "成功" : "失败");
            map.put("errorMessage", log.getErrorMessage());
            map.put("createTime", log.getCreateTime() == null ? "" : log.getCreateTime().format(DATE_TIME_FORMATTER));
            return map;
        }).toList();

        response.setBufferSize(8192);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");

        String fileName = "我的操作日志.xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);

        try (OutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {

            writer.addHeaderAlias("username", "操作账号");
            writer.addHeaderAlias("title", "操作模块");
            writer.addHeaderAlias("operationType", "操作类型");
            writer.addHeaderAlias("requestMethod", "请求方式");
            writer.addHeaderAlias("requestUri", "请求地址");
            writer.addHeaderAlias("ipAddress", "客户端IP");
            writer.addHeaderAlias("browser", "浏览器");
            writer.addHeaderAlias("operatingSystem", "操作系统");
            writer.addHeaderAlias("requestParams", "请求参数");
            writer.addHeaderAlias("operateStatus", "操作状态");
            writer.addHeaderAlias("errorMessage", "异常信息");
            writer.addHeaderAlias("createTime", "操作时间");

            writer.setOnlyAlias(true);
            writer.write(exportDataList, true);
            writer.flush(out, false);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}




