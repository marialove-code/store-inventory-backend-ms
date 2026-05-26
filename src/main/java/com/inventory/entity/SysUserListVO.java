package com.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class SysUserListVO {

    /**
     * 雪花算法ID
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
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 性别 0未知 1男 2女
     */
    private Integer sex;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 账号状态 1正常 0禁用
     */
    private Integer status;

    /**
     * 排序序号
     */
    private Integer sort;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime updateTime;

}