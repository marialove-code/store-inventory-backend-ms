package com.inventory.modules.own.home.controller;

import com.inventory.common.response.Result;
import com.inventory.modules.auth.service.UserSessionService;
import com.inventory.modules.own.home.service.ProfileService;
import com.inventory.modules.own.home.vo.ProfileOverviewVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心Controller
 */
@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Resource
    private ProfileService profileService;
    @Resource
    private UserSessionService userSessionService;

    /**
     * 获取个人主页总览数据（核心接口）
     */
    @GetMapping("/overview")
    public Result<ProfileOverviewVO> overview() {
        return Result.success(profileService.getOverview());
    }

    /**
     * 下线其他所有在线设备
     */
    @GetMapping("/kick-others")
    public Result<Integer> kickOthers() {
        return Result.success(userSessionService.kickOtherDevices());
    }
}