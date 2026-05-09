package com.inventory.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 系统用户表
 * @TableName sys_user
 */
@TableName(value ="sys_user")
@Data
public class SysUser {
    /**
     * 雪花算法ID
     */
    @TableId
    private Long id;

    /**
     * 登录用户名
     */
    private String userName;

    /**
     * BCrypt加密密码
     */
    private String password;

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
     * 逻辑删除 0未删 1已删
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}