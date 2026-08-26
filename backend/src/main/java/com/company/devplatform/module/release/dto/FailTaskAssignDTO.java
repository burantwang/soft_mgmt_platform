package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 指派失败聚合任务请求（id 由路径参数提供）
 */
@Data
public class FailTaskAssignDTO {

    private Long id;

    /** 责任人（用户ID） */
    @NotNull(message = "请选择责任人")
    private Long assigneeId;
}
