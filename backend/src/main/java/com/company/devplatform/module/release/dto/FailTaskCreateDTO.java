package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 手动创建失败聚合任务请求
 */
@Data
public class FailTaskCreateDTO {

    /** 关联发布记录ID（可选，须为失败记录且未被其他任务关联） */
    private Long recordId;

    /** 失败概述 */
    @NotBlank(message = "失败概述不能为空")
    @Size(max = 500, message = "失败概述不能超过500字")
    private String summary;

    /** 失败原因（汇总） */
    @Size(max = 1000, message = "失败原因不能超过1000字")
    private String failReason;

    /** 修改方案（汇总） */
    @Size(max = 1000, message = "修改方案不能超过1000字")
    private String fixPlan;

    /** 责任人（用户ID） */
    private Long assigneeId;

    /** 失败用例明细（可选） */
    private List<FailCaseItemDTO> cases;

    @Data
    public static class FailCaseItemDTO {

        /** 用例类型:failed失败 error错误 */
        private String caseType;

        /** 用例名称 */
        @NotBlank(message = "用例名称不能为空")
        private String caseName;

        /** 用例运行日志 */
        private String caseLog;
    }
}
