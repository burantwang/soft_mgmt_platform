package com.company.devplatform.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleDTO {

    /** 编辑时必传 */
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String remark;

    /** 状态:1正常 0停用 */
    private Integer status;
}
