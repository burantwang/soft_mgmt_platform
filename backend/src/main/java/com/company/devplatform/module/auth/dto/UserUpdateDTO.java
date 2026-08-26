package com.company.devplatform.module.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String nickname;

    private String email;

    private String phone;

    private Integer status;

    private String remark;

    /** 角色ID列表,为空数组表示清空角色 */
    private List<Long> roleIds;
}
