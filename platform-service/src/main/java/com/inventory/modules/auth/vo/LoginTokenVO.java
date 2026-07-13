package com.inventory.modules.auth.vo;

import com.inventory.modules.system.user.vo.SysUserSimpleVO;
import lombok.Data;

// 新建 LoginTokenVO
@Data
public class LoginTokenVO {
    private String token;
    private String accessToken;
    private String refreshToken;
    private SysUserSimpleVO user;


}