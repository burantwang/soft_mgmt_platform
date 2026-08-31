package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 失败用例处理/更新请求
 */
@Data
public class FailCaseUpdateDTO {

    /** 目标状态：1待处理 2处理中 3已完成 4已关闭 */
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    /** 责任人ID */
    private Long assigneeId;

    /** 失败原因 */
    @Size(max = 1000, message = "失败原因不能超过1000字")
    private String failReason;

    /** 修改方案 */
    @Size(max = 1000, message = "修改方案不能超过1000字")
    private String fixPlan;

    /** 是否提Bug:0否 1是 */
    private Integer isBug;

    /** 分析进展（无长度限制） */
    private String progress;

    /** 结论 */
    @Size(max = 1000, message = "结论不能超过1000字")
    private String conclusion;

    /** AI辅助分析描述（AI 生成，原因分析、修改建议等） */
    @Size(max = 10000, message = "AI分析描述不能超过10000字")
    private String aiAnalysis;

    /** AI分析是否正确:1是 0否 */
    private Integer aiAnalysisCorrect;

    /** Bug单号(Redmine) */
    private String bugNo;

    /** 问题分类 */
    private String issueCategory;
}
