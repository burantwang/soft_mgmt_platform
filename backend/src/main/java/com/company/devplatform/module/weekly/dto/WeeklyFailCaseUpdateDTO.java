package com.company.devplatform.module.weekly.dto;

import lombok.Data;

/**
 * WeeklySanity 失败用例更新表单
 */
@Data
public class WeeklyFailCaseUpdateDTO {

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
    private Integer status;

    /** 责任人 */
    private Long assigneeId;

    /** 失败原因 */
    private String failReason;

    /** 修改方案 */
    private String fixPlan;

    /** 是否提Bug:0否 1是 */
    private Integer isBug;

    /** 分析进展 */
    private String progress;

    /** 结论 */
    private String conclusion;

    /** AI辅助分析描述 */
    private String aiAnalysis;

    /** AI分析是否正确:1是 0否 */
    private Integer aiAnalysisCorrect;

    /** Bug单号(Redmine) */
    private String bugNo;

    /** 问题分类 */
    private String issueCategory;
}
