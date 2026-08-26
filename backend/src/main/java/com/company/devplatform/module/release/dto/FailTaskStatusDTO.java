package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 失败聚合任务状态流转请求（id 由路径参数提供）
 */
@Data
public class FailTaskStatusDTO {

    private Long id;

    /** 目标状态：1待处理 2处理中 3已完成 4已关闭 */
    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
