package com.inventory.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationTypeEnum {
    LOGIN("用户登录"),
    LOGOUT("退出登录"),
    REGISTER("用户注册"),
    ADD("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    QUERY("查询");

    private final String desc;
}