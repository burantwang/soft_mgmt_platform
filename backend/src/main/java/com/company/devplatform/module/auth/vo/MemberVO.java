package com.company.devplatform.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组内成员展示对象(用户基础信息,不含敏感字段)
 */
@Data
public class MemberVO {

    private Long id;

    /** 登录账号 */
    private String username;

    /** 姓名 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 状态:1正常 0禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
