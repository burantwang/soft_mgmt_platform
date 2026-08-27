package com.company.devplatform.module.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 问题单任务 状态流转
 */
@Data
public class IssueTaskStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
