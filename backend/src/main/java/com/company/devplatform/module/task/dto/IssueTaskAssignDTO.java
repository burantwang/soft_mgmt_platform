package com.company.devplatform.module.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 问题单任务 指派
 */
@Data
public class IssueTaskAssignDTO {

    @NotNull(message = "责任人不能为空")
    private Long assigneeId;
}
