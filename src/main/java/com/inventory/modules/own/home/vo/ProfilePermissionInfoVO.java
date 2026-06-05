package com.inventory.modules.own.home.vo;

import lombok.Data;
import java.util.List;

@Data
public class ProfilePermissionInfoVO {
    private List<String> roleNames;      // 角色名称列表
    private Integer permissionTotal;    // 菜单/权限总数
    private List<MenuTreeVO> menuTree;  // 菜单树
}