package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目(机型)新增/编辑入参
 */
@Data
public class ReleaseProjectDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "项目/机型名称不能为空")
    @Size(max = 64, message = "项目名称最长64字符")
    private String projectName;

    @NotBlank(message = "项目编码不能为空")
    @Size(max = 64, message = "项目编码最长64字符")
    private String projectCode;

    @Size(max = 255, message = "描述最长255字符")
    private String description;

    /** 状态:1启用 0停用 */
    private Integer status;
}
