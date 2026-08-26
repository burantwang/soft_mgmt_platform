package com.company.devplatform.module.auth.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录结果 VO
 */
@Data
public class LoginResultVO {

    /** 登录令牌 */
    private String token;

    /** 用户信息 */
    private UserVO userInfo;

    /** 权限码列表(含"*"表示全部) */
    private List<String> permissions;

    /** 角色编码列表 */
    private List<String> roles;
}
