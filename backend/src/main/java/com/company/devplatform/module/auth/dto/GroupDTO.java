package com.company.devplatform.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 组新建/编辑参数
 */
@Data
public class GroupDTO {

    /** 组ID(编辑时必填) */
    private Long id;

    /** 组名(如 软件研发一处) */
    @NotBlank(message = "组名不能为空")
    @Size(max = 64, message = "组名不能超过64字符")
    private String groupName;

    /** 备注 */
    @Size(max = 255, message = "备注不能超过255字符")
    private String remark;

    /** 状态:1启用 0停用 */
    private Integer status;
}
