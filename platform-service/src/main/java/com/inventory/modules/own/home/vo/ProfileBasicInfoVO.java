package com.inventory.modules.own.home.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 个人中心 - 基础信息VO
 * 全部用于前端页面展示，不含敏感字段
 */
@Data
public class ProfileBasicInfoVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号（用户名）
     */
    private String userName;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 性别 0-未知 1-男 2-女
     */
    private String sex;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 账号状态 0-正常 1-停用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 所属角色（多个角色用逗号拼接）
     */
    private String roleName;
}