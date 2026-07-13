package com.inventory.modules.system.permission.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 前端菜单树形结构VO
 * 用于返回给前端渲染侧边栏菜单
 */
@Data
public class MenuVO {

    /**
     * 菜单/权限ID
     */
    private Long id;

    /**
     * 父菜单ID（顶级菜单为0）
     */
    private Long parentId;

    /**
     * 菜单名称（前端侧边栏显示的名字）
     */
    private String permName;

    /**
     * 路由地址（前端跳转路径）
     */
    private String path;

    /**
     * 前端组件路径（对应.vue文件位置）
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 权限类型
     * M=目录 C=菜单 F=按钮（前端只渲染M/C）
     */
    private String permType;

    /**
     * 权限标识（用于接口权限控制，如：system:user:list）
     */
    private String permCode;

    /**
     * 排序号（越小越靠前）
     */
    private Integer sort;


    /**
     * 账号状态 1正常 0禁用
     */
    private Integer status;

    /**
     * 子菜单列表（递归树形结构）
     */
    private List<MenuVO> children;


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