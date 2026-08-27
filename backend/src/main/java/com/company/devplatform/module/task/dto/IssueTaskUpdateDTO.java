package com.company.devplatform.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 问题单任务 编辑（标题/描述/原因/方案/优先级/关联信息）
 */
@Data
public class IssueTaskUpdateDTO {

    @NotNull(message = "任务ID不能为空")
    private Long id;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private Integer priority = 2;

    private String branch;

    private String version;

    private String projectName;

    private String summary;

    private String reason;

    private String fixPlan;
}
