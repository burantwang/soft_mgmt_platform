package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理失败用例明细请求
 */
@Data
public class FailCaseHandleDTO {

    @NotNull(message = "用例ID不能为空")
    private Long caseId;

    /** 目标状态：2处理中 3已修复 4非缺陷 */
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    /** 失败原因 */
    @Size(max = 1000, message = "失败原因不能超过1000字")
    private String failReason;

    /** 修改方案 */
    @Size(max = 1000, message = "修改方案不能超过1000字")
    private String fixPlan;
}
