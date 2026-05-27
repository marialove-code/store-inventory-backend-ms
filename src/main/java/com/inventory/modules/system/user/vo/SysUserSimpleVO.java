package com.inventory.modules.system.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
public class SysUserSimpleVO {

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private String id;

    /**
     * 登录用户名
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
     * 用户角色
     */
    private List<String> roles;

    /**
     * 用户权限
     */
    private List<String> permissions;

}