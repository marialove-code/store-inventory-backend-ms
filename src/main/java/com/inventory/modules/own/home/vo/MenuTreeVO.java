package com.inventory.modules.own.home.vo;

import lombok.Data;
import java.util.List;

@Data
public class MenuTreeVO {
    private Long id;
    private Long parentId;
    private String permName;
    private List<MenuTreeVO> children;
}