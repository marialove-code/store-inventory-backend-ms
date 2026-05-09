package com.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 系统角色表
 * @TableName sys_role
 */
@TableName(value ="sys_role")
@Data
public class SysRole {
    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码（唯一）
     */
    private String roleCode;

    /**
     * 显示排序
     */
    private Integer sort;

    /**
     * 状态 1=正常 0=禁用
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;

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

    /**
     * 删除标识 0=未删除 1=已删除
     */
    private Integer isDeleted;


}