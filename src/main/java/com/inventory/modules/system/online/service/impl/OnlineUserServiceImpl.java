package com.inventory.modules.system.online.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.modules.auth.vo.LoginUserVO;
import com.inventory.modules.system.online.entity.OnlineUser;
import com.inventory.modules.system.monitor.vo.RedisKeyVO;
import com.inventory.modules.system.monitor.vo.RedisMonitorVO;
import com.inventory.modules.system.online.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.inventory.common.constants.RedisConstants.LOGIN_TOKEN_PREFIX;

/**
 * 在线用户 Service 实现
 * 从 Redis 中读取登录token，实现在线用户展示 + 强制下线
 */
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 只获取【access】token，完美匹配你的登录代码
    private static final String TOKEN_PREFIX = "user:token:*:access:*";

    @Override
    public Page<OnlineUser> pageOnlineUser(String keyword, Long pageNum, Long pageSize) {

        // 只获取 access token
        Set<String> keys = redisTemplate.keys(TOKEN_PREFIX);
        if (keys == null || keys.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        List<OnlineUser> onlineUserList = new ArrayList<>();

        for (String key : keys) {
            Object val = redisTemplate.opsForValue().get(key);

            // 🔥 安全判断：只处理 LoginUserVO，其他全部跳过
            if (!(val instanceof LoginUserVO loginUser)) {
                continue;
            }

            OnlineUser onlineUser = new OnlineUser();
            onlineUser.setTokenKey(key);
            onlineUser.setUserId(loginUser.getUserId());
            onlineUser.setUserName(loginUser.getUsername());
            onlineUser.setNickName(loginUser.getNickName());
            onlineUser.setIpaddr(loginUser.getIpaddr());
            onlineUser.setBrowser(loginUser.getBrowser());
            onlineUser.setOs(loginUser.getOs());
            onlineUser.setLoginTime(loginUser.getLoginTime());
            onlineUser.setExpireTime(loginUser.getExpireTime());

            onlineUserList.add(onlineUser);
        }

        // 搜索
        if (StringUtils.hasText(keyword)) {
            onlineUserList = onlineUserList.stream()
                    .filter(u ->
                            (u.getUserName() != null && u.getUserName().contains(keyword)) ||
                                    (u.getIpaddr() != null && u.getIpaddr().contains(keyword)) ||
                                    (u.getNickName() != null && u.getNickName().contains(keyword))
                    )
                    .collect(Collectors.toList());
        }

        // 分页
        int start = (int) ((pageNum - 1) * pageSize);
        int end = Math.min(start + pageSize.intValue(), onlineUserList.size());
        List<OnlineUser> pageRecords = onlineUserList.subList(start, end);

        Page<OnlineUser> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page.setTotal(onlineUserList.size());
        page.setRecords(pageRecords);

        return page;
    }

    @Override
    public boolean forceLogout(String tokenKey) {
        Boolean exist = redisTemplate.hasKey(tokenKey);
        if (Boolean.TRUE.equals(exist)) {
            redisTemplate.delete(tokenKey);
            return true;
        }
        return false;
    }


    @Override
    public RedisMonitorVO getRedisMonitorInfo() {
        RedisMonitorVO vo = new RedisMonitorVO();
        try {
            // 1. 检测 Redis 连接（正确方式）
            String ping = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            vo.setConnectStatus("PONG".equals(ping) ? "连接正常" : "连接异常");

            // 2. 统计在线用户 key 数量
            Set<String> onlineKeys = redisTemplate.keys(LOGIN_TOKEN_PREFIX+"*:access:*");
            long onlineCount = (onlineKeys == null) ? 0L : onlineKeys.size();
            vo.setOnlineUserCount(onlineCount);

            // 3. 获取 Redis 信息
            Properties info = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.info());

            // 4. 内存占用
            String usedMemory = info.getProperty("used_memory_human", "0B");
            vo.setUsedMemory(usedMemory);

            // 5. Key 总数（解析 db0 信息）
            String db0Info = info.getProperty("db0", "keys=0");
            String keyCountStr = db0Info.split(",")[0].split("=")[1];
            long totalKeys = Long.parseLong(keyCountStr);
            vo.setTotalKey(totalKeys);

        } catch (Exception e) {
            // 异常兜底，防止前端报错
            vo.setConnectStatus("连接异常");
            vo.setOnlineUserCount(0L);
            vo.setUsedMemory("0B");
            vo.setTotalKey(0L);
        }
        return vo;
    }

    @Override
    public Set<String> searchRedisKey(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return new HashSet<>();
        }
        return redisTemplate.keys("*" + keyword + "*");
    }

    @Override
    public Boolean deleteRedisKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return redisTemplate.delete(key);
    }

    @Override
    public Page<RedisKeyVO> pageRedisKey(String keyword, Long pageNum, Long pageSize) {
        try {
            // 1. 拼接模糊查询
            String pattern = StrUtil.isBlank(keyword) ? "*" : "*" + keyword + "*";
            Set<String> keySet = redisTemplate.keys(pattern);
            if (keySet == null) keySet = new HashSet<>();

            // 2. 转成列表并排序（让结果稳定）
            List<String> keyList = new ArrayList<>(keySet);
            Collections.sort(keyList);

            // 3. 手动分页
            long start = (pageNum - 1) * pageSize;
            long end = Math.min(start + pageSize, keyList.size());

            List<RedisKeyVO> records = new ArrayList<>();
            for (long i = start; i < end; i++) {
                RedisKeyVO vo = new RedisKeyVO();
                vo.setKey(keyList.get((int) i));
                records.add(vo);
            }

            // 4. 封装 Page
            Page<RedisKeyVO> page = new Page<>();
            page.setCurrent(pageNum);
            page.setSize(pageSize);
            page.setTotal(keyList.size());
            page.setRecords(records);

            return page;
        } catch (Exception e) {
            return new Page<>();
        }
    }


}