package com.company.devplatform.module.weekly.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * WeeklySanity 失败用例分组明细项
 */
@Data
public class WeeklyFailCaseVO {

    private Long id;

    /** 周度报告ID */
    private Long reportId;

    /** 所属模块(HTML文件名) */
    private String moduleName;

    /** 用例类型:failed失败 error错误 */
    private String caseType;

    private String caseTypeDesc;

    private String caseName;

    private String caseLog;

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
    private Integer status;

    private String statusDesc;

    private Long assigneeId;

    private String assigneeName;

    private String failReason;

    private String fixPlan;

    private Integer isBug;

    private String progress;

    private String conclusion;

    /** AI辅助分析描述（原因分析、修改建议等） */
    private String aiAnalysis;

    /** AI分析是否正确:1是 0否 */
    private Integer aiAnalysisCorrect;

    /** AI分析根因 */
    private String aiRootCause;

    /** AI分析佐证 */
    private String aiEvidence;

    /** AI解决建议 */
    private String aiSolution;

    /** Bug单号(Redmine) */
    private String bugNo;

    /** 问题分类 */
    private String issueCategory;

    /** 报告上传时间 */
    private LocalDateTime publishTime;
}
