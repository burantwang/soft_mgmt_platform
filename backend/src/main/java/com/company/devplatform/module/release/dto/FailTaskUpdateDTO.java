package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑失败聚合任务请求（概述/原因/方案）
 */
@Data
public class FailTaskUpdateDTO {

    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Size(max = 500, message = "失败概述不能超过500字")
    private String summary;

    @Size(max = 1000, message = "失败原因不能超过1000字")
    private String failReason;

    @Size(max = 1000, message = "修改方案不能超过1000字")
    private String fixPlan;
}
