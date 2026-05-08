package com.inventory.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysUserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String userName;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickName; // 可为空
}