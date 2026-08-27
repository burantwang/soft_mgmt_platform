package com.company.devplatform.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务类型 新增/编辑
 */
@Data
public class TaskTypeDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "类型编码不能为空")
    private String code;

    @NotBlank(message = "类型名称不能为空")
    private String name;

    private String icon;

    private Integer sort = 0;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;
}
