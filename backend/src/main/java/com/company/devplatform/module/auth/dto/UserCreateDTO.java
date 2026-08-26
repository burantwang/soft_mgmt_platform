package com.company.devplatform.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateDTO {

    @NotBlank(message = "登录账号不能为空")
    private String username;

    /** 初始密码,为空时使用默认密码 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String nickname;

    private String email;

    private String phone;

    private String remark;

    /** 角色ID列表 */
    private List<Long> roleIds;
}
