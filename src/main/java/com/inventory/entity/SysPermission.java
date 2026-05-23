package com.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 系统权限表
 * @TableName sys_permission
 */
@TableName(value ="sys_permission")
@Data
public class SysPermission {

    @TableId
    private Long id;

    private Long parentId;

    private String permName;

    private String permType; // M=目录 C=菜单 F=按钮

    private String path;

    private String component;

    private String permCode; // system:user:list

    private String icon;

    private Integer sort;

    private Integer status;
    /**
     * 创建时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime createTime;
    /**
     * 创建时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

}