package com.company.devplatform.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 问题单任务 创建
 */
@Data
public class IssueTaskCreateDTO {

    @NotNull(message = "任务类型不能为空")
    private Long taskTypeId;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    /** 优先级:1低 2中 3高 */
    private Integer priority = 2;

    private String branch;

    private String version;

    private String projectName;

    /** 责任人(用户ID) */
    private Long assigneeId;

    private String summary;

    private String reason;

    private String fixPlan;

    /** 明细项（可多个） */
    private List<IssueItemDTO> items = new ArrayList<>();
}
