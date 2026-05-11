package com.inventory.entity.login;

import com.inventory.entity.SysUserSimpleVO;
import lombok.Data;

// 新建 LoginTokenVO
@Data
public class LoginTokenVO {
    private String token;
    private String accessToken;
    private String refreshToken;
    private SysUserSimpleVO user;
}