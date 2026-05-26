package com.inventory.entity.menu;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色列表分页返回VO
 */
@Data
public class SysRoleListVO {

    /**
     * 角色主键ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色唯一编码
     */
    private String roleCode;

    /**
     * 角色备注说明
     */
    private String remark;

    /**
     * 角色状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    /**
     * 创建时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime createTime;
}