package com.inventory.modules.system.log.service.impl;


import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.framework.security.context.LoginUserContext;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.system.log.entity.SysLoginLog;
import com.inventory.modules.system.log.mapper.SysLoginLogMapper;
import com.inventory.modules.system.log.service.SysLoginLogService;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录日志Service实现类
 *
 * @author yourname
 * @date 2026-05-25
 */
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {
    private final SysLoginLogMapper sysLoginLogMapper;
    // 时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<SysLoginLog> pageLoginLog(String userName, String nickName, String loginIp, Integer loginStatus,
                                          String beginTime, String endTime, Long pageNum, Long pageSize) {
        // 构建分页对象
        Page<SysLoginLog> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询：登录账号
        if (StringUtils.hasText(userName)) {
            queryWrapper.like(SysLoginLog::getUserName, userName);
        }
        // 模糊查询：用户昵称
        if (StringUtils.hasText(nickName)) {
            queryWrapper.like(SysLoginLog::getNickName, nickName);
        }
        // 模糊查询：登录IP
        if (StringUtils.hasText(loginIp)) {
            queryWrapper.like(SysLoginLog::getLoginIp, loginIp);
        }
        // 精确查询：登录状态
        if (loginStatus != null) {
            queryWrapper.eq(SysLoginLog::getLoginStatus, loginStatus);
        }
        // 时间范围：开始时间
        if (StringUtils.hasText(beginTime)) {
            LocalDateTime begin = LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER);
            queryWrapper.ge(SysLoginLog::getLoginTime, begin);
        }
        // 时间范围：结束时间
        if (StringUtils.hasText(endTime)) {
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
            queryWrapper.le(SysLoginLog::getLoginTime, end);
        }
        // 按登录时间倒序排序
        queryWrapper.orderByDesc(SysLoginLog::getLoginTime);

        // 执行分页查询
        return sysLoginLogMapper.selectPage(page, queryWrapper);
    }

    @Override
    public void exportLoginLog(HttpServletResponse response,
                               String userName,
                               String nickName,
                               String loginIp,
                               Integer loginStatus,
                               String beginTime,
                               String endTime) throws IOException {
        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userName)) {
            queryWrapper.like(SysLoginLog::getUserName, userName);
        }
        if (StringUtils.hasText(nickName)) {
            queryWrapper.like(SysLoginLog::getNickName, nickName);
        }
        if (StringUtils.hasText(loginIp)) {
            queryWrapper.like(SysLoginLog::getLoginIp, loginIp);
        }
        if (loginStatus != null) {
            queryWrapper.eq(SysLoginLog::getLoginStatus, loginStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.ge(SysLoginLog::getLoginTime,
                    LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(SysLoginLog::getLoginTime,
                    LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }
        queryWrapper.orderByDesc(SysLoginLog::getLoginTime);
        List<SysLoginLog> originList = sysLoginLogMapper.selectList(queryWrapper);
        List<Map<String, Object>> exportDataList = originList.stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userName", log.getUserName());
            map.put("nickName", log.getNickName());
            map.put("loginIp", log.getLoginIp());
            map.put("loginAddress", log.getLoginAddress());
            map.put("browser", log.getBrowser());
            map.put("operatingSystem", log.getOperatingSystem());
            map.put("loginStatus", Objects.equals(log.getLoginStatus(), 1) ? "成功" : "失败");
            map.put("failReason", log.getFailReason());
            // LocalDateTime 必须转字符串
            map.put("loginTime", log.getLoginTime() == null
                    ? ""
                    : log.getLoginTime().format(DATE_TIME_FORMATTER));
            return map;
        }).toList();
        // 清空缓冲区，避免之前写入的内容污染
//        response.reset();
        response.setBufferSize(8192);
        // 正确的 Excel MIME
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        // 文件名（兼容中文）
        String fileName = "登录日志.xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        try (OutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.addHeaderAlias("userName", "用户账号");
            writer.addHeaderAlias("nickName", "用户昵称");
            writer.addHeaderAlias("loginIp", "登录IP");
            writer.addHeaderAlias("loginAddress", "登录地点");
            writer.addHeaderAlias("browser", "浏览器");
            writer.addHeaderAlias("operatingSystem", "操作系统");
            writer.addHeaderAlias("loginStatus", "登录状态");
            writer.addHeaderAlias("failReason", "失败原因");
            writer.addHeaderAlias("loginTime", "登录时间");
            // 只导出指定列
            writer.setOnlyAlias(true);
            writer.write(exportDataList, true);
            // false：不要在这里关 response 的输出流
            writer.flush(out, false);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearLoginLog() {
        // 清空所有登录日志（物理删除，若需逻辑删除可改为更新del_flag）
        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        sysLoginLogMapper.delete(queryWrapper);
    }


    @Override
    public Page<SysLoginLog> pageLoginLogMy(String loginIp, Integer loginStatus,
                                            String beginTime, String endTime,
                                            Long pageNum, Long pageSize) {
        // 获取当前登录用户（只查自己）
        LoginUserVO loginUser = LoginUserContext.getUser();
        Long userId = loginUser.getUserId();

        Page<SysLoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();

        // 只查当前用户
        queryWrapper.eq(SysLoginLog::getUserId, userId);

        if (StringUtils.hasText(loginIp)) {
            queryWrapper.like(SysLoginLog::getLoginIp, loginIp);
        }
        if (loginStatus != null) {
            queryWrapper.eq(SysLoginLog::getLoginStatus, loginStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.ge(SysLoginLog::getLoginTime, LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(SysLoginLog::getLoginTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }

        queryWrapper.orderByDesc(SysLoginLog::getLoginTime);
        return sysLoginLogMapper.selectPage(page, queryWrapper);
    }

    @Override
    public void exportLoginLogMy(HttpServletResponse response, String loginIp, Integer loginStatus,
                                 String beginTime, String endTime) throws IOException {
        // 获取当前登录用户（只导出自己）
        LoginUserVO loginUser = LoginUserContext.getUser();
        Long userId = loginUser.getUserId();

        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        // 只查当前用户
        queryWrapper.eq(SysLoginLog::getUserId, userId);

        if (StringUtils.hasText(loginIp)) {
            queryWrapper.like(SysLoginLog::getLoginIp, loginIp);
        }
        if (loginStatus != null) {
            queryWrapper.eq(SysLoginLog::getLoginStatus, loginStatus);
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.ge(SysLoginLog::getLoginTime, LocalDateTime.parse(beginTime, DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.le(SysLoginLog::getLoginTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
        }

        queryWrapper.orderByDesc(SysLoginLog::getLoginTime);
        List<SysLoginLog> originList = sysLoginLogMapper.selectList(queryWrapper);

        List<Map<String, Object>> exportDataList = originList.stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userName", log.getUserName());
            map.put("nickName", log.getNickName());
            map.put("loginIp", log.getLoginIp());
            map.put("loginAddress", log.getLoginAddress());
            map.put("browser", log.getBrowser());
            map.put("operatingSystem", log.getOperatingSystem());
            map.put("loginStatus", Objects.equals(log.getLoginStatus(), 1) ? "成功" : "失败");
            map.put("failReason", log.getFailReason());
            map.put("loginTime", log.getLoginTime() == null ? "" : log.getLoginTime().format(DATE_TIME_FORMATTER));
            return map;
        }).toList();

        response.setBufferSize(8192);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");

        String fileName = "我的登录日志.xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);

        try (OutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {

            writer.addHeaderAlias("userName", "用户账号");
            writer.addHeaderAlias("nickName", "用户昵称");
            writer.addHeaderAlias("loginIp", "登录IP");
            writer.addHeaderAlias("loginAddress", "登录地点");
            writer.addHeaderAlias("browser", "浏览器");
            writer.addHeaderAlias("operatingSystem", "操作系统");
            writer.addHeaderAlias("loginStatus", "登录状态");
            writer.addHeaderAlias("failReason", "失败原因");
            writer.addHeaderAlias("loginTime", "登录时间");

            writer.setOnlyAlias(true);
            writer.write(exportDataList, true);
            writer.flush(out, false);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}